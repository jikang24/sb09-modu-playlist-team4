package com.mopl.domain.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReviewTest {

  private UUID contentId;
  private UUID userId;

  private Review makeReview(BigDecimal rating) {
    contentId = UUID.randomUUID();
    userId = UUID.randomUUID();
    return Review.create(contentId, userId, rating, "좋아요", "테스터", "https://image.jpg");
  }

  @Nested
  @DisplayName("리뷰 생성 - Review.create()")
  class Create {

    @Test
    @DisplayName("정상 생성 - 필드가 모두 채워지고 createdAt과 updatedAt이 같다")
    void success() {
      Review review = makeReview(BigDecimal.valueOf(4.5));

      assertThat(review.getId()).isNotNull();
      assertThat(review.getContentId()).isEqualTo(contentId);
      assertThat(review.getUserId()).isEqualTo(userId);
      assertThat(review.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
      assertThat(review.getText()).isEqualTo("좋아요");
      assertThat(review.getAuthorName()).isEqualTo("테스터");
      assertThat(review.getAuthorProfileImageUrl()).isEqualTo("https://image.jpg");
      assertThat(review.getCreatedAt()).isEqualTo(review.getUpdatedAt());
    }

    @Test
    @DisplayName("경계값 - 평점 0.5와 5는 정상 생성된다")
    void success_boundaryRating() {
      assertThat(makeReview(BigDecimal.valueOf(0.5)).getRating())
          .isEqualByComparingTo(BigDecimal.valueOf(0.5));
      assertThat(makeReview(BigDecimal.valueOf(5)).getRating())
          .isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    @DisplayName("contentId가 null이면 예외 발생")
    void fail_nullContentId() {
      assertThatThrownBy(() -> Review.create(
          null, UUID.randomUUID(), BigDecimal.valueOf(3), "text", "name", "img"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("userId가 null이면 예외 발생")
    void fail_nullUserId() {
      assertThatThrownBy(() -> Review.create(
          UUID.randomUUID(), null, BigDecimal.valueOf(3), "text", "name", "img"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("rating이 null이면 예외 발생")
    void fail_nullRating() {
      assertThatThrownBy(() -> Review.create(
          UUID.randomUUID(), UUID.randomUUID(), null, "text", "name", "img"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("rating이 0.5 미만이면 예외 발생 (DB CHECK 제약과 동일 범위)")
    void fail_ratingBelowMin() {
      assertThatThrownBy(() -> makeReview(BigDecimal.valueOf(0.4)))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("rating이 음수이면 예외 발생")
    void fail_ratingNegative() {
      assertThatThrownBy(() -> makeReview(BigDecimal.valueOf(-0.1)))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("rating이 5 초과이면 예외 발생")
    void fail_ratingAboveFive() {
      assertThatThrownBy(() -> makeReview(BigDecimal.valueOf(5.1)))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }
  }

  @Nested
  @DisplayName("리뷰 수정 - update()")
  class Update {

    @Test
    @DisplayName("정상 수정 - rating/text가 바뀌고 updatedAt이 갱신된다")
    void success() throws InterruptedException {
      Review review = makeReview(BigDecimal.valueOf(3));
      var originalUpdatedAt = review.getUpdatedAt();

      Thread.sleep(5);
      review.update(BigDecimal.valueOf(4.5), "수정된 리뷰");

      assertThat(review.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
      assertThat(review.getText()).isEqualTo("수정된 리뷰");
      assertThat(review.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("잘못된 rating으로 수정 시도 - 예외 발생하고 기존 값은 유지된다")
    void fail_invalidRating() {
      Review review = makeReview(BigDecimal.valueOf(3));

      assertThatThrownBy(() -> review.update(BigDecimal.valueOf(9), "수정된 리뷰"))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));

      assertThat(review.getRating()).isEqualByComparingTo(BigDecimal.valueOf(3));
      assertThat(review.getText()).isEqualTo("좋아요");
    }
  }

  @Nested
  @DisplayName("작성자 스냅샷 갱신 - updateAuthorSnapshot()")
  class UpdateAuthorSnapshot {

    @Test
    @DisplayName("이름/프로필이미지만 바뀌고 updatedAt은 그대로다")
    void success() {
      Review review = makeReview(BigDecimal.valueOf(3));
      var originalUpdatedAt = review.getUpdatedAt();

      review.updateAuthorSnapshot("새이름", "https://new.jpg");

      assertThat(review.getAuthorName()).isEqualTo("새이름");
      assertThat(review.getAuthorProfileImageUrl()).isEqualTo("https://new.jpg");
      assertThat(review.getUpdatedAt()).isEqualTo(originalUpdatedAt);
    }
  }
}
