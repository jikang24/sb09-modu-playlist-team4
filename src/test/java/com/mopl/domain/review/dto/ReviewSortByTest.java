package com.mopl.domain.review.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewSortByTest {

  @Test
  @DisplayName("null이면 기본값 CREATED_AT을 반환한다")
  void from_null_returnsDefault() {
    assertThat(ReviewSortBy.from(null)).isEqualTo(ReviewSortBy.CREATED_AT);
  }

  @Test
  @DisplayName("이미 대문자/스네이크 형식이면 그대로 매핑된다")
  void from_alreadyNormalized() {
    assertThat(ReviewSortBy.from("CREATED_AT")).isEqualTo(ReviewSortBy.CREATED_AT);
    assertThat(ReviewSortBy.from("RATING")).isEqualTo(ReviewSortBy.RATING);
  }

  @Test
  @DisplayName("소문자 camelCase(createdAt)도 스네이크케이스로 변환해 매핑한다")
  void from_camelCase() {
    assertThat(ReviewSortBy.from("createdAt")).isEqualTo(ReviewSortBy.CREATED_AT);
    assertThat(ReviewSortBy.from("rating")).isEqualTo(ReviewSortBy.RATING);
  }

  @Test
  @DisplayName("알 수 없는 값이면 INVALID_SORT_BY 예외")
  void from_unknownValue_throws() {
    assertThatThrownBy(() -> ReviewSortBy.from("unknown"))
        .isInstanceOf(MoplException.class)
        .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_SORT_BY));
  }
}