package com.mopl.domain.watchingsession.scheduler;

import com.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시청 세션 고아 데이터 정리 배치.
 *
 * disconnect 이벤트(STOMP UNSUBSCRIBE/DISCONNECT) 유실 시 세션 Hash/역인덱스는 TTL로
 * 자연 소멸하지만, 콘텐츠 인덱스 ZSet은 여러 세션이 공유하는 키라 TTL을 걸 수 없어
 * 만료된 세션의 멤버가 그대로 남는다 - 주기적으로 스캔해서 제거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionCleanupScheduler {

  private final WatchingSessionRepository watchingSessionRepository;

  @Scheduled(cron = "${watching-session.cleanup.cron:0 */30 * * * *}")
  public void cleanup() {
    long removed = watchingSessionRepository.cleanupOrphanSessions();
    if (removed > 0) {
      log.info("시청세션 고아 인덱스 정리 완료. {}건 제거", removed);
    }
  }
}