package com.mopl.global.outbox;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
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

  @PostConstruct
  void initialized() {
    log.info("OutboxRelayScheduler initialized");
  }

  @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:1000}")
  public void relay() {
    log.info("Outbox relay tick");
    List<OutboxEvent> claimedEvents = outboxClaimService.claimBatch(BATCH_SIZE);
    if (claimedEvents.isEmpty()) {
      return;
    }
    log.info("Outbox relay 시작 - {}건", claimedEvents.size());
    claimedEvents.forEach(this::publish);
  }

  private void publish(OutboxEvent event) {
    log.info("Kafka 발행 요청 - eventId={}, topic={}, key={}",
        event.getId(), event.getTopic(), event.getMessageKey());
    CompletableFuture<SendResult<String, String>> future =
        kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload());

    future.whenComplete((result, ex) -> {
      if (ex == null) {
        log.info("Kafka 발행 완료 - eventId={}, topic={}", event.getId(), event.getTopic());
        outboxStatusUpdater.markPublished(event.getId());
      } else {
        log.error("아웃박스 이벤트 발행 실패. id={}, topic={}, eventType={}",
            event.getId(), event.getTopic(), event.getEventType(), ex);
        outboxStatusUpdater.markFailed(event.getId(), MAX_RETRY);
      }
    });
  }
}
