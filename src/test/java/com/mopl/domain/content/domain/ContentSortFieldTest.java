package com.mopl.domain.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentSortFieldTest {

  @Test
  @DisplayName("propertyName() - 각 enum 상수가 매핑된 엔티티 필드명을 반환한다")
  void propertyName_returnsMappedFieldName() {
    assertThat(ContentSortField.CREATED_AT.propertyName()).isEqualTo("createdAt");
    assertThat(ContentSortField.REVIEW_COUNT.propertyName()).isEqualTo("reviewCount");
    assertThat(ContentSortField.AVERAGE_RATING.propertyName()).isEqualTo("averageRating");
  }

  @Test
  @DisplayName("resolve() - watcherCount/reviewCount는 REVIEW_COUNT로 매핑된다")
  void resolve_reviewCountAliases() {
    assertThat(ContentSortField.resolve("watcherCount")).isEqualTo(ContentSortField.REVIEW_COUNT);
    assertThat(ContentSortField.resolve("reviewCount")).isEqualTo(ContentSortField.REVIEW_COUNT);
  }

  @Test
  @DisplayName("resolve() - rate/averageRating은 AVERAGE_RATING으로 매핑된다")
  void resolve_averageRatingAliases() {
    assertThat(ContentSortField.resolve("rate")).isEqualTo(ContentSortField.AVERAGE_RATING);
    assertThat(ContentSortField.resolve("averageRating")).isEqualTo(ContentSortField.AVERAGE_RATING);
  }

  @Test
  @DisplayName("resolve() - 그 외 값(null 포함)은 기본값 CREATED_AT으로 매핑된다")
  void resolve_defaultsToCreatedAt() {
    assertThat(ContentSortField.resolve("createdAt")).isEqualTo(ContentSortField.CREATED_AT);
    assertThat(ContentSortField.resolve("title")).isEqualTo(ContentSortField.CREATED_AT);
    assertThat(ContentSortField.resolve(null)).isEqualTo(ContentSortField.CREATED_AT);
  }
}