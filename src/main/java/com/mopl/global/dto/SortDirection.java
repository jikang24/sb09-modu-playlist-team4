package com.mopl.global.dto;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;

public enum SortDirection { ASCENDING, DESCENDING ;

  // 문자열(요청 파라미터)로부터 안전하게 변환하는 팩토리 메서드
  public static SortDirection from(String value) {
    try {
      return SortDirection.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new MoplException(ErrorCode.INVALID_SORT_DIRECTION);
    }
  }

}
