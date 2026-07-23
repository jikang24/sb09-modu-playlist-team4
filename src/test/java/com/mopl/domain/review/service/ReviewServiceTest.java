package com.mopl.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.mopl.domain.review.adapter.port.LoadUserPort;
import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewDto;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.domain.review.dto.ReviewSortBy;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private ReviewService reviewService;

  private Review makeReview(UUID contentId, UUID userId, BigDecimal rating) {
    return Review.create(contentId, userId, rating, "좋아요", "작성자", "https://image.jpg");
  }

  @Nested
  @DisplayName("리뷰 생성 - createReview()")
  class CreateReview {

    @Test
    @DisplayName("정상 생성 - User 조회 후 저장하고 평점 갱신 이벤트를 발행한다")
    void success() {
      UUID contentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UserSummary author = new UserSummary(userId, "작성자", "https://image.jpg");

      given(reviewRepository.existsByContentIdAndUserId(contentId, userId)).willReturn(false);
      given(loadUserPort.getUserSummary(userId)).willReturn(author);

      UUID reviewId = reviewService.createReview(contentId, userId, BigDecimal.valueOf(4.5), "좋아요");

      assertThat(reviewId).isNotNull();
      then(reviewRepository).should().saveAndFlush(any(Review.class));

      ArgumentCaptor<ReviewRatingUpdatedEvent> captor =
          ArgumentCaptor.forClass(ReviewRatingUpdatedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().contentId()).isEqualTo(contentId);
      assertThat(captor.getValue().ratingDelta()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
      assertThat(captor.getValue().countDelta()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 리뷰를 작성한 경우 - 예외 발생하고 User 조회/저장은 일어나지 않는다")
    void fail_alreadyExists() {
      UUID contentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      given(reviewRepository.existsByContentIdAndUserId(contentId, userId)).willReturn(true);

      assertThatThrownBy(() ->
          reviewService.createReview(contentId, userId, BigDecimal.valueOf(4), "좋아요"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));

      then(loadUserPort).shouldHaveNoInteractions();
      then(reviewRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("동시 요청 - 선검증 통과 후 저장 시점에 unique 제약 위반이면 500 대신 REVIEW_ALREADY_EXISTS로 변환된다")
    void fail_concurrentDuplicate() {
      UUID contentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UserSummary author = new UserSummary(userId, "작성자", "https://image.jpg");

      given(reviewRepository.existsByContentIdAndUserId(contentId, userId)).willReturn(false);
      given(loadUserPort.getUserSummary(userId)).willReturn(author);
      given(reviewRepository.saveAndFlush(any(Review.class)))
          .willThrow(new DataIntegrityViolationException("duplicate key"));

      assertThatThrownBy(() ->
          reviewService.createReview(contentId, userId, BigDecimal.valueOf(4), "좋아요"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("rating이 유효 범위를 벗어나면 예외 발생")
    void fail_invalidRating() {
      UUID contentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UserSummary author = new UserSummary(userId, "작성자", "https://image.jpg");

      given(reviewRepository.existsByContentIdAndUserId(contentId, userId)).willReturn(false);
      given(loadUserPort.getUserSummary(userId)).willReturn(author);

      assertThatThrownBy(() ->
          reviewService.createReview(contentId, userId, BigDecimal.valueOf(10), "좋아요"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));

      then(reviewRepository).should(org.mockito.Mockito.never()).save(any());
    }
  }

  @Nested
  @DisplayName("리뷰 수정 - updateReview()")
  class UpdateReview {

    @Test
    @DisplayName("정상 수정 - 소유자면 수정되고 평점 변화량(delta)만큼 갱신 이벤트를 발행한다")
    void success() {
      UUID userId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      Review review = makeReview(contentId, userId, BigDecimal.valueOf(3));

      given(reviewRepository.findByIdForUpdate(review.getId())).willReturn(Optional.of(review));

      reviewService.updateReview(review.getId(), userId, BigDecimal.valueOf(4.5), "수정된 리뷰");

      assertThat(review.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
      assertThat(review.getText()).isEqualTo("수정된 리뷰");

      ArgumentCaptor<ReviewRatingUpdatedEvent> captor =
          ArgumentCaptor.forClass(ReviewRatingUpdatedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().contentId()).isEqualTo(contentId);
      assertThat(captor.getValue().ratingDelta()).isEqualByComparingTo(BigDecimal.valueOf(1.5)); // 4.5 - 3
      assertThat(captor.getValue().countDelta()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 - REVIEW_NOT_FOUND 예외")
    void fail_notFound() {
      UUID reviewId = UUID.randomUUID();
      given(reviewRepository.findByIdForUpdate(reviewId)).willReturn(Optional.empty());

      assertThatThrownBy(() ->
          reviewService.updateReview(reviewId, UUID.randomUUID(), BigDecimal.valueOf(4), "text"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 수정 시도 - REVIEW_NOT_OWNER 예외")
    void fail_notOwner() {
      UUID ownerId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      Review review = makeReview(UUID.randomUUID(), ownerId, BigDecimal.valueOf(3));

      given(reviewRepository.findByIdForUpdate(review.getId())).willReturn(Optional.of(review));

      assertThatThrownBy(() ->
          reviewService.updateReview(review.getId(), otherUserId, BigDecimal.valueOf(4), "text"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_NOT_OWNER));

      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("리뷰 삭제 - deleteReview()")
  class DeleteReview {

    @Test
    @DisplayName("정상 삭제 - 소유자면 삭제되고 삭제된 rating만큼 음수 delta로 갱신 이벤트를 발행한다")
    void success() {
      UUID userId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      Review review = makeReview(contentId, userId, BigDecimal.valueOf(3));

      given(reviewRepository.findByIdForUpdate(review.getId())).willReturn(Optional.of(review));

      reviewService.deleteReview(review.getId(), userId);

      then(reviewRepository).should().delete(review);

      ArgumentCaptor<ReviewRatingUpdatedEvent> captor =
          ArgumentCaptor.forClass(ReviewRatingUpdatedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().contentId()).isEqualTo(contentId);
      assertThat(captor.getValue().ratingDelta()).isEqualByComparingTo(BigDecimal.valueOf(-3));
      assertThat(captor.getValue().countDelta()).isEqualTo(-1);
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 - REVIEW_NOT_FOUND 예외")
    void fail_notFound() {
      UUID reviewId = UUID.randomUUID();
      given(reviewRepository.findByIdForUpdate(reviewId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> reviewService.deleteReview(reviewId, UUID.randomUUID()))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));

      then(reviewRepository).should(org.mockito.Mockito.never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 삭제 시도 - REVIEW_NOT_OWNER 예외")
    void fail_notOwner() {
      UUID ownerId = UUID.randomUUID();
      UUID otherUserId = UUID.randomUUID();
      Review review = makeReview(UUID.randomUUID(), ownerId, BigDecimal.valueOf(3));

      given(reviewRepository.findByIdForUpdate(review.getId())).willReturn(Optional.of(review));

      assertThatThrownBy(() -> reviewService.deleteReview(review.getId(), otherUserId))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.REVIEW_NOT_OWNER));

      then(reviewRepository).should(org.mockito.Mockito.never()).delete(any(Review.class));
    }
  }

  @Nested
  @DisplayName("리뷰 목록 조회 - getReviews()")
  class GetReviews {

    private ReviewSearchRequest makeRequest(int limit, String sortBy, String sortDirection) {
      ReviewSortBy sortByEnum = sortBy == null ? null : ReviewSortBy.from(sortBy);
      return new ReviewSearchRequest(
          UUID.randomUUID(), null, null, limit, sortByEnum, SortDirection.from(sortDirection));
    }


    private void mockFindAll(List<Review> reviews) {
      given(reviewRepository.findAllByCondition(any(ReviewSearchRequest.class)))
          .willReturn(reviews);
    }

    @Test
    @DisplayName("다음 페이지 없음 - hasNext=false")
    void noNextPage() {
      ReviewSearchRequest request = makeRequest(3, null, "DESCENDING");
      Review review = makeReview(request.contentId(), UUID.randomUUID(), BigDecimal.valueOf(4));

      mockFindAll(List.of(review));
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class)))
          .willReturn(1L);

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      assertThat(response.data()).hasSize(1);
      assertThat(response.hasNext()).isFalse();
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();
      assertThat(response.sortBy()).isEqualTo("CREATED_AT");
      assertThat(response.totalCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("다음 페이지 있음 - createdAt 정렬 기준 nextCursor가 채워진다")
    void hasNextPage_createdAt() {
      ReviewSearchRequest request = makeRequest(1, "createdAt", "DESCENDING");
      Review first = makeReview(request.contentId(), UUID.randomUUID(), BigDecimal.valueOf(4));
      Review second = makeReview(request.contentId(), UUID.randomUUID(), BigDecimal.valueOf(3));

      mockFindAll(List.of(first, second)); // limit(1)+1=2건
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class)))
          .willReturn(5L);

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      assertThat(response.data()).hasSize(1);
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isEqualTo(first.getCreatedAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(first.getId());
      assertThat(response.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("rating 정렬 기준이면 nextCursor가 rating 문자열이다")
    void hasNextPage_rating() {
      ReviewSearchRequest request = makeRequest(1, "rating", "DESCENDING");
      Review first = makeReview(request.contentId(), UUID.randomUUID(), BigDecimal.valueOf(4.5));
      Review second = makeReview(request.contentId(), UUID.randomUUID(), BigDecimal.valueOf(3.5));

      mockFindAll(List.of(first, second));
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class)))
          .willReturn(2L);

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      assertThat(response.nextCursor()).isEqualTo(first.getRating().toString());
      assertThat(response.sortBy()).isEqualTo("RATING");
    }

    @Test
    @DisplayName("빈 결과 - data 비어있고 hasNext=false")
    void emptyResult() {
      ReviewSearchRequest request = makeRequest(10, null, "DESCENDING");

      mockFindAll(List.of());
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class)))
          .willReturn(0L);

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      assertThat(response.data()).isEmpty();
      assertThat(response.hasNext()).isFalse();
      assertThat(response.totalCount()).isZero();
    }

    @Test
    @DisplayName("ReviewDto 변환 - author 정보가 스냅샷에서 그대로 매핑된다")
    void mapsAuthorSnapshot() {
      ReviewSearchRequest request = makeRequest(10, null, "DESCENDING");
      UUID userId = UUID.randomUUID();
      Review review = makeReview(request.contentId(), userId, BigDecimal.valueOf(4));

      mockFindAll(List.of(review));
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class)))
          .willReturn(1L);

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      ReviewDto dto = response.data().get(0);
      assertThat(dto.author().userId()).isEqualTo(userId);
      assertThat(dto.author().name()).isEqualTo("작성자");
      assertThat(dto.author().profileImageUrl()).isEqualTo("https://image.jpg");
      assertThat(dto.rating()).isEqualByComparingTo(BigDecimal.valueOf(4));
      then(s3Service).shouldHaveNoInteractions(); // 스킴이 있는 외부 URL은 그대로 반환, presign 호출 없음
    }

    @Test
    @DisplayName("ReviewDto 변환 - 저장된 값이 S3 key(스킴 없음)면 조회 시점에 presigned URL로 치환한다")
    void mapsAuthorSnapshot_resolvesS3KeyToPresignedUrl() {
      ReviewSearchRequest request = makeRequest(10, null, "DESCENDING");
      UUID userId = UUID.randomUUID();
      Review review = Review.create(
          request.contentId(), userId, BigDecimal.valueOf(4), "좋아요", "작성자", "profile-key.jpg");

      mockFindAll(List.of(review));
      given(reviewRepository.countByCondition(any(ReviewSearchRequest.class))).willReturn(1L);
      given(s3Service.getPresignedUrl("profile-key.jpg"))
          .willReturn("https://signed.example.com/profile-key.jpg?sig=abc");

      CursorPageResponse<ReviewDto> response = reviewService.getReviews(request);

      assertThat(response.data().get(0).author().profileImageUrl())
          .isEqualTo("https://signed.example.com/profile-key.jpg?sig=abc");
    }
  }
}
