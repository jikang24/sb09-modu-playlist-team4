package com.mopl.domain.review.dto;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;

public enum ReviewSortBy {
  CREATED_AT,
  RATING;

  public static ReviewSortBy from(String value) {
    if (value == null) {
      return CREATED_AT; // 기본값
    }
    // 프론트가 camelCase("createdAt")로 보내는 경우까지 받아주기 위한 변환
    String normalized = value
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .toUpperCase();
    try {
      return ReviewSortBy.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new MoplException(ErrorCode.INVALID_SORT_BY);
    }
  }
}