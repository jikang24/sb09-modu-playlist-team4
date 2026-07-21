package com.mopl.global.outbox;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRY = 5;

  private final OutboxClaimService outboxClaimService;
  private final OutboxStatusUpdater outboxStatusUpdater;

  @Qualifier("outboxKafkaTemplate")
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:1000}")
  public void relay() {
    List<OutboxEvent> claimedEvents = outboxClaimService.claimBatch(BATCH_SIZE);
    if (claimedEvents.isEmpty()) {
      return;
    }
    log.debug("아웃박스 이벤트 {}건 발행 시작", claimedEvents.size());
    claimedEvents.forEach(this::publish);
  }

  private void publish(OutboxEvent event) {
    CompletableFuture<SendResult<String, String>> future =
        kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload());

    future.whenComplete((result, ex) -> {
      if (ex == null) {
        outboxStatusUpdater.markPublished(event.getId());
      } else {
        log.error("아웃박스 이벤트 발행 실패. id={}, topic={}, eventType={}",
            event.getId(), event.getTopic(), event.getEventType(), ex);
        outboxStatusUpdater.markFailed(event.getId(), MAX_RETRY);
      }
    });
  }
}