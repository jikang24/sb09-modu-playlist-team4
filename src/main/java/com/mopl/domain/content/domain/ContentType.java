package com.mopl.domain.content.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentType {
  MOVIE,
  TV_SERIES,
  SPORT;

  // 프론트가 camelCase("tvSeries")로 보내는 경우까지 받아주기 위한 변환
  @JsonCreator
  public static ContentType from(String value) {
    String normalized = value
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .toUpperCase();
    return ContentType.valueOf(normalized);
  }

  // 응답 직렬화도 프론트가 쓰는 camelCase("tvSeries")로 내려줘야 프론트의 라벨 매핑({movie, tvSeries, sport})과 매칭됨
  // (그냥 두면 기본 직렬화로 "TV_SERIES"가 나가서 프론트에서 유형이 안 보임)
  @JsonValue
  public String toJson() {
    return switch (this) {
      case MOVIE -> "movie";
      case TV_SERIES -> "tvSeries";
      case SPORT -> "sport";
    };
  }
}