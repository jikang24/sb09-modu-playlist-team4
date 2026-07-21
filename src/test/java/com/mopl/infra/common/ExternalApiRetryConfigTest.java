package com.mopl.infra.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.global.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

@DisplayName("ExternalApiRetryConfig 테스트")
class ExternalApiRetryConfigTest {

  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final RetryTemplate retryTemplate =
      new ExternalApiRetryConfig().externalApiRetryTemplate(meterRegistry);

  @Test
  @DisplayName("재시도 가능한 예외로 실패했다가 성공하면 실패 횟수만큼 external.api.retry 카운터가 증가한다")
  void retryableFailureThenSuccess_incrementsRetryCounter() throws Exception {
    AtomicInteger attempt = new AtomicInteger();

    String result = retryTemplate.execute(ctx -> {
      if (attempt.getAndIncrement() < 2) {
        throw new ExternalApiRetryableException(ErrorCode.TMDB_RATE_LIMITED);
      }
      return "성공";
    });

    assertThat(result).isEqualTo("성공");
    assertThat(attempt.get()).isEqualTo(3); // 실패 2회 + 성공 1회
    assertThat(meterRegistry.counter("external.api.retry", "errorCode", ErrorCode.TMDB_RATE_LIMITED.name())
        .count()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("재시도 대상이 아닌 예외는 재시도 없이 즉시 전파되고 카운터도 늘지 않는다")
  void nonRetryableException_propagatesImmediatelyWithoutCounting() {
    AtomicInteger attempt = new AtomicInteger();

    assertThatThrownBy(() -> retryTemplate.execute(ctx -> {
      attempt.incrementAndGet();
      throw new IllegalStateException("재시도 대상 아님");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(attempt.get()).isEqualTo(1);
    assertThat(meterRegistry.counter("external.api.retry", "errorCode", "IllegalStateException").count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("최대 시도 횟수를 다 채우고도 실패하면 마지막 예외가 전파되고, 실패 횟수만큼 카운터가 증가한다")
  void allAttemptsFail_propagatesLastExceptionAndCountsEveryFailure() {
    assertThatThrownBy(() -> retryTemplate.execute(ctx -> {
      throw new ExternalApiRetryableException(ErrorCode.TMDB_SERVER_ERROR);
    })).isInstanceOf(ExternalApiRetryableException.class);

    assertThat(meterRegistry.counter("external.api.retry", "errorCode", ErrorCode.TMDB_SERVER_ERROR.name())
        .count()).isEqualTo(4.0); // MAX_ATTEMPTS(최초 1회 + 재시도 3회)만큼 실패 기록
  }
}