package com.mopl.domain.content.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

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
}