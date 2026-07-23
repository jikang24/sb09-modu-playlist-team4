package com.mopl.domain.review.service;

import com.mopl.domain.review.adapter.port.LoadUserPort;
import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewDto;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.domain.review.dto.ReviewSortBy;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReviewUseCase 인터페이스 없이 바로 구현체 하나만 둠
 * (테스트할 땐 @SpringBootTest나 Mockito로 이 클래스 자체를 mock/spy 하면 되니
 *  인터페이스가 없어도 테스트 어려워지지 않음)
 */
/** ReviewUseCase 인터페이스 없이 구현체 하나만 둠 (모듈러 모놀리스 단순화 방향) */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final LoadUserPort loadUserPort;
  private final S3Service s3Service;

  public UUID createReview(UUID contentId, UUID userId, BigDecimal rating, String text) {
    // 1인 1리뷰 검증 - DB unique 제약이 최종 방어선, 이건 친절한 에러 메시지용 1차 검증
    if (reviewRepository.existsByContentIdAndUserId(contentId, userId)) {
      throw new MoplException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // 리뷰 작성 시점에 딱 한 번 User 모듈 조회 (정말 어쩔 수 없는 직접 호출 지점)
    // 이후 목록 조회에서는 이 호출이 다시 일어나지 않음 - 아래 저장된 스냅샷을 그대로 씀
    UserSummary author = loadUserPort.getUserSummary(userId);

    Review review = Review.create(
        contentId, userId, rating, text, author.name(), author.profileImageUrl());
    try {
      // saveAndFlush로 즉시 flush해서, 위 선검증을 통과한 두 요청이 동시에 들어와도
      // UNIQUE(user_id, content_id) 위반이 이 지점에서 바로 터지게 한다.
      // (그냥 save()면 flush가 늦춰져서 뒤에서 다른 예외로 새어나갈 수 있음)
      reviewRepository.saveAndFlush(review);
    } catch (DataIntegrityViolationException e) {
      throw new MoplException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    eventPublisher.publishEvent(new ReviewRatingUpdatedEvent(contentId, rating, 1));
    return review.getId();
  }

  public void updateReview(UUID reviewId, UUID userId, BigDecimal rating, String text) {
    // FOR UPDATE로 조회 - 동시 수정 요청(더블클릭/재전송)이 같은 previousRating을 보고
    // 각자 delta를 계산하면 content 평점에 중복 반영되므로, 같은 리뷰 행에 대한 동시
    // 수정을 트랜잭션 단위로 직렬화해 뒤 요청이 앞 요청의 최신 rating을 보게 한다.
    Review review = reviewRepository.findByIdForUpdate(reviewId)
        .orElseThrow(() -> new MoplException(ErrorCode.REVIEW_NOT_FOUND));
    validateOwner(review, userId);

    BigDecimal previousRating = review.getRating();

    // save() 호출 없음 - review는 이미 영속 상태라, 필드만 바꾸면 트랜잭션 끝날 때
    // JPA가 dirty checking으로 알아서 UPDATE 쿼리를 날려줌
    review.update(rating, text);

    BigDecimal ratingDelta = rating.subtract(previousRating);
    eventPublisher.publishEvent(new ReviewRatingUpdatedEvent(review.getContentId(), ratingDelta, 0));
  }

  public void deleteReview(UUID reviewId, UUID userId) {
    // updateReview와 동시에 들어오면(수정 중인 review를 그대로 삭제) 잠금 없이 읽은
    // rating이 최신 값이 아닐 수 있으므로, 동일하게 FOR UPDATE로 직렬화한다.
    Review review = reviewRepository.findByIdForUpdate(reviewId)
        .orElseThrow(() -> new MoplException(ErrorCode.REVIEW_NOT_FOUND));
    validateOwner(review, userId);

    UUID contentId = review.getContentId(); // 삭제 전에 미리 꺼내둠
    BigDecimal ratingDelta = review.getRating().negate();
    reviewRepository.delete(review);

    eventPublisher.publishEvent(new ReviewRatingUpdatedEvent(contentId, ratingDelta, -1));
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<ReviewDto> getReviews(ReviewSearchRequest request) {
    ReviewSortBy sortBy = request.sortBy() != null ? request.sortBy() : ReviewSortBy.CREATED_AT;

    // limit+1개 조회해서 hasNext 판단 (Content 쪽과 동일 패턴)
    List<Review> reviews = reviewRepository.findAllByCondition(request);

    boolean hasNext = reviews.size() > request.limit();
    List<Review> content = hasNext ? reviews.subList(0, request.limit()) : reviews;

    // Review 엔티티에 이미 author 스냅샷이 있으니, User 모듈을 호출하지 않고 바로 DTO로 변환
    List<ReviewDto> reviewDtos = content.stream()
        .map(r -> new ReviewDto(
            r.getId(), r.getContentId(),
            new ReviewDto.AuthorDto(
                r.getUserId(), r.getAuthorName(), resolveAuthorImageUrl(r.getAuthorProfileImageUrl())),
            r.getText(), r.getRating()))
        .toList();

    long totalCount = reviewRepository.countByCondition(request);

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !content.isEmpty()) {
      Review last = content.get(content.size() - 1);
      nextCursor = sortBy == ReviewSortBy.RATING
          ? last.getRating().toString()
          : last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    return new CursorPageResponse<>(
        reviewDtos, nextCursor, nextIdAfter, hasNext, totalCount,
        sortBy.name(), request.sortDirection().name());
  }

  // 저장된 값(스냅샷)에 스킴("://")이 없으면 우리가 업로드한 S3 key이므로 presigned URL로 치환하고,
  // 스킴이 있으면(소셜 로그인 프로필 이미지 등 외부 URL) 그대로 반환한다.
  private String resolveAuthorImageUrl(String stored) {
    if (stored == null || stored.isBlank() || stored.contains("://")) {
      return stored;
    }
    return s3Service.getPresignedUrl(stored);
  }

  private void validateOwner(Review review, UUID userId) {
    if (!review.getUserId().equals(userId)) {
      throw new MoplException(ErrorCode.REVIEW_NOT_OWNER);
    }
  }
}