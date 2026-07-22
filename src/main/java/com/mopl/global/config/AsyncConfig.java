package com.mopl.global.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Async 메서드(주로 AFTER_COMMIT 이벤트 리스너)는 반환값이 void라 예외가 호출자에게 절대
 * 전파되지 않는다. 커스텀 핸들러 없이는 Spring 기본 SimpleAsyncUncaughtExceptionHandler가
 * "Unexpected exception occurred invoking async method..."처럼 어떤 메서드/이벤트인지
 * 특정하기 어려운 로그만 남겨서, 실제 장애(예: 리뷰 평점 갱신 실패) 원인을 로그에서 못 찾는
 * 사고가 있었다 - 메서드명/인자를 포함해 검색 가능한 형태로 남기도록 재정의한다.
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (Throwable ex, Method method, Object... params) ->
        log.error("[Async] 비동기 메서드 실행 실패 - {}.{}({}), args={}",
            method.getDeclaringClass().getSimpleName(), method.getName(),
            Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .toList(),
            Arrays.toString(params), ex);
  }

  /**
   * 팔로우 활동 알림(FollowActivityPlaylistListener, FollowActivityWatchingSessionListener) 전용 실행기.
   * 특히 실시간 시청은 콘텐츠를 자주 갈아탈 때마다 팔로워 수만큼 팬아웃이 발생할 수 있어,
   * 기본 SimpleAsyncTaskExecutor(태스크마다 새 스레드, 무제한)로 두면 폭주 시 스레드가 무한정 늘어날 수 있다.
   * 큐/풀이 가득 찼을 때는 CallerRunsPolicy로 제출한 스레드가 직접 실행하게 한다 - 알림이 유실되면 안 되므로
   * 폭주 시 호출 스레드가 잠깐 느려질 수 있지만, 평소엔 그대로 비동기로 처리된다.
   */
  @Bean
  public ThreadPoolTaskExecutor followActivityTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("follow-activity-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
    return executor;
  }
}
