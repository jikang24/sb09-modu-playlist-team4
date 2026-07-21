package com.mopl.infra.outbox;

import com.mopl.global.outbox.OutboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

  private static final int RETENTION_DAYS = 7;
  private static final int STUCK_PROCESSING_MINUTES = 10;

  private final OutboxRepository outboxRepository;

  @Scheduled(cron = "${outbox.cleanup.cron:0 0 4 * * *}")
  @Transactional
  public void cleanup() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
    int deleted = outboxRepository.deletePublishedBefore(threshold);
    if (deleted > 0) {
      log.info("아웃박스 정리 완료. {}건 삭제 (기준: {} 이전 발행분)", deleted, threshold);
    }
  }

  /**
   * relay 인스턴스가 카프카 send 도중 죽어 PROCESSING 상태로 멈춰버린 이벤트를
   * 다시 PENDING으로 되돌려 재시도될 수 있게 한다.
   */
  @Scheduled(fixedDelayString = "${outbox.recover.fixed-delay-ms:60000}")
  @Transactional
  public void recoverStuckProcessing() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_PROCESSING_MINUTES);
    int recovered = outboxRepository.recoverStuckProcessing(threshold);
    if (recovered > 0) {
      log.warn("PROCESSING 상태로 멈춰있던 아웃박스 이벤트 {}건을 PENDING으로 복구했습니다.", recovered);
    }
  }
}