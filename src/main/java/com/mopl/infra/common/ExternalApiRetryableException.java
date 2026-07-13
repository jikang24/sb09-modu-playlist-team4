package com.mopl.infra.common;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;

/**
 * 외부 API 호출 실패 중 재시도로 복구될 가능성이 있는 경우(429, 5xx)에 던지는 예외
 * ExternalApiRetryConfig의 RetryTemplate이 이 타입만 재시도 대상으로 분류함
 */
public class ExternalApiRetryableException extends MoplException {
  public ExternalApiRetryableException(ErrorCode errorCode) {
    super(errorCode);
  }
}