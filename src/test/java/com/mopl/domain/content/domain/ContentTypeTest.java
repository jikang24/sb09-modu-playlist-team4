package com.mopl.domain.content.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContentTypeTest {

  @Nested
  @DisplayName("from() - 문자열 → ContentType 변환")
  class From {

    @Test
    @DisplayName("이미 스네이크/대문자 형식이면 그대로 매핑된다")
    void from_alreadyNormalized() {
      assertThat(ContentType.from("MOVIE")).isEqualTo(ContentType.MOVIE);
      assertThat(ContentType.from("TV_SERIES")).isEqualTo(ContentType.TV_SERIES);
      assertThat(ContentType.from("SPORT")).isEqualTo(ContentType.SPORT);
    }

    @Test
    @DisplayName("소문자 camelCase(tvSeries)도 스네이크케이스로 변환해 매핑한다")
    void from_camelCase() {
      assertThat(ContentType.from("movie")).isEqualTo(ContentType.MOVIE);
      assertThat(ContentType.from("tvSeries")).isEqualTo(ContentType.TV_SERIES);
      assertThat(ContentType.from("sport")).isEqualTo(ContentType.SPORT);
    }

    @Test
    @DisplayName("알 수 없는 값이면 IllegalArgumentException")
    void from_unknownValue_throws() {
      assertThatThrownBy(() -> ContentType.from("unknown"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("toJson() - ContentType → 프론트 camelCase 문자열")
  class ToJson {

    @Test
    @DisplayName("MOVIE → movie")
    void movie() {
      assertThat(ContentType.MOVIE.toJson()).isEqualTo("movie");
    }

    @Test
    @DisplayName("TV_SERIES → tvSeries")
    void tvSeries() {
      assertThat(ContentType.TV_SERIES.toJson()).isEqualTo("tvSeries");
    }

    @Test
    @DisplayName("SPORT → sport")
    void sport() {
      assertThat(ContentType.SPORT.toJson()).isEqualTo("sport");
    }
  }
}