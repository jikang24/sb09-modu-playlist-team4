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
}