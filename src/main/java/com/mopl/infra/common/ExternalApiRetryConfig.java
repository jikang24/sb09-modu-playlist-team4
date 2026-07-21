package com.mopl.infra.common;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * TMDB, SportsDB 등 외부 API 호출 공용 재시도 설정
 *
 * 429/5xx처럼 일시적으로 실패해도 재요청하면 성공할 수 있는 경우에 한해 지수 백오프로 재시도.
 * 400/401 등 요청 자체가 잘못된 경우는 재시도해도 결과가 같으므로 대상에서 제외(ExternalApiRetryableException만 재시도).
 */
@Slf4j
@Configuration
public class ExternalApiRetryConfig {

  private static final int MAX_ATTEMPTS = 4; // 최초 시도 1회 + 재시도 3회
  private static final long INITIAL_INTERVAL_MS = 500;
  private static final long MAX_INTERVAL_MS = 4000;
  private static final double BACKOFF_MULTIPLIER = 2.0;

  @Bean
  public RetryTemplate externalApiRetryTemplate(MeterRegistry meterRegistry) {
    RetryTemplate retryTemplate = new RetryTemplate();

    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(
        MAX_ATTEMPTS,
        Collections.singletonMap(ExternalApiRetryableException.class, true)
    ));

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(INITIAL_INTERVAL_MS);
    backOffPolicy.setMaxInterval(MAX_INTERVAL_MS);
    backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    retryTemplate.registerListener(new RetryListener() {
      @Override
      public <T, E extends Throwable> void onError(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("[ExternalAPI] 재시도 {}회차 실패 - {}",
            context.getRetryCount(), throwable.getMessage());

        String errorCode = throwable instanceof ExternalApiRetryableException retryable
            ? retryable.getErrorCode().name()
            : throwable.getClass().getSimpleName();
        meterRegistry.counter("external.api.retry", "errorCode", errorCode).increment();
      }
    });

    return retryTemplate;
  }
}