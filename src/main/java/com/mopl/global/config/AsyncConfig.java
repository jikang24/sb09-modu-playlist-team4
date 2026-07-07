package com.mopl.global.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  /**
   * 팔로우 활동 알림(FollowActivityPlaylistListener, FollowActivityWatchingSessionListener) 전용 실행기.
   * 특히 실시간 시청은 콘텐츠를 자주 갈아탈 때마다 팔로워 수만큼 팬아웃이 발생할 수 있어,
   * 기본 SimpleAsyncTaskExecutor(태스크마다 새 스레드, 무제한)로 두면 폭주 시 스레드가 무한정 늘어날 수 있다.
   * 큐/풀이 가득 차면 가장 오래된 대기 작업을 버리고 새 작업을 받는다 - 알림은 유실돼도 되는 정보이므로
   * 요청/STOMP 스레드를 절대 막지 않는 쪽을 우선한다.
   */
  @Bean
  public ThreadPoolTaskExecutor followActivityTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("follow-activity-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    executor.initialize();
    return executor;
  }
}
