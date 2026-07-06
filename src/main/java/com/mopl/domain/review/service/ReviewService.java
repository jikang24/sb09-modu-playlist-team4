package com.mopl.domain.review.service;

import com.mopl.domain.review.adapter.port.LoadUserPort;
import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewDto;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.domain.review.repository.ReviewSpecification;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    reviewRepository.save(review);

    publishRatingUpdatedEvent(contentId);
    return review.getId();
  }

  public void updateReview(UUID reviewId, UUID userId, BigDecimal rating, String text) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new MoplException(ErrorCode.REVIEW_NOT_FOUND));
    validateOwner(review, userId);

    // save() 호출 없음 - review는 이미 영속 상태라, 필드만 바꾸면 트랜잭션 끝날 때
    // JPA가 dirty checking으로 알아서 UPDATE 쿼리를 날려줌
    review.update(rating, text);

    publishRatingUpdatedEvent(review.getContentId());
  }

  public void deleteReview(UUID reviewId, UUID userId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new MoplException(ErrorCode.REVIEW_NOT_FOUND));
    validateOwner(review, userId);

    UUID contentId = review.getContentId(); // 삭제 전에 미리 꺼내둠
    reviewRepository.delete(review);

    publishRatingUpdatedEvent(contentId); // 마지막 리뷰였다면 평균 0, 개수 0으로 반영됨
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<ReviewDto> getReviews(ReviewSearchRequest request) {
    boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
    Sort.Direction direction = isAscending ? Sort.Direction.ASC : Sort.Direction.DESC;
    String sortBy = request.sortBy() != null ? request.sortBy() : "createdAt";

    // limit+1개 조회해서 hasNext 판단 (Content 쪽과 동일 패턴)
    PageRequest pageRequest = PageRequest.of(0, request.limit() + 1,
        Sort.by(direction, sortBy).and(Sort.by(direction, "id")));

    List<Review> reviews = reviewRepository
        .findAll(ReviewSpecification.byCondition(request), pageRequest)
        .getContent();

    boolean hasNext = reviews.size() > request.limit();
    List<Review> content = hasNext ? reviews.subList(0, request.limit()) : reviews;

    // Review 엔티티에 이미 author 스냅샷이 있으니, User 모듈을 호출하지 않고 바로 DTO로 변환
    List<ReviewDto> reviewDtos = content.stream()
        .map(r -> new ReviewDto(
            r.getId(), r.getContentId(),
            new ReviewDto.AuthorDto(r.getUserId(), r.getAuthorName(), r.getAuthorProfileImageUrl()),
            r.getText(), r.getRating()))
        .toList();

    long totalCount = reviewRepository.count(ReviewSpecification.byCondition(request));

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !content.isEmpty()) {
      Review last = content.get(content.size() - 1);
      nextCursor = "rating".equals(sortBy)
          ? last.getRating().toString()
          : last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    return new CursorPageResponse<>(
        reviewDtos, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, request.sortDirection());
  }

  private void validateOwner(Review review, UUID userId) {
    if (!review.getUserId().equals(userId)) {
      throw new MoplException(ErrorCode.FORBIDDEN);
    }
  }

  /** 재집계 방식 - create/update/delete 세 곳에서 공통 호출 (하나라도 빠지면 평점이 안 맞게 됨) */
  private void publishRatingUpdatedEvent(UUID contentId) {
    Object[] result = reviewRepository.aggregateRatingStats(contentId);
    Double avg = (Double) result[0];
    Long count = (Long) result[1];

    BigDecimal averageRating = (avg == null)
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

    eventPublisher.publishEvent(
        new ReviewRatingUpdatedEvent(contentId, averageRating, count.intValue()));
  }
}