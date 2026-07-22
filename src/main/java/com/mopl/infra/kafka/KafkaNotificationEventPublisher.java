package com.mopl.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import com.mopl.global.outbox.OutboxEvent;
import com.mopl.global.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public void publish(NotificationRequestedEvent event) {
    log.info(
        "txActive={}, txName={}",
        TransactionSynchronizationManager.isActualTransactionActive(),
        TransactionSynchronizationManager.getCurrentTransactionName()
    );

    String receiverId = event.receiverId().toString();

    OutboxEvent outbox = OutboxEvent.builder()
        .aggregateType("NOTIFICATION")
        .aggregateId(event.receiverId())
        .eventType("NOTIFICATION_REQUESTED")
        .topic(NotificationTopics.NOTIFICATION_REQUESTED)
        .messageKey(receiverId)
        .payload(writeValueAsString(event))
        .build();

    log.info("=== Outbox 저장 시작 ===");

    OutboxEvent saved = outboxRepository.save(outbox);

    log.info(
        "Outbox 저장 완료 - eventId={}, receiverId={}, outboxId={}",
        event.eventId(),
        event.receiverId(),
        saved.getId()
    );

    log.info("EntityManager.contains(saved) = {}", entityManager.contains(saved));
    log.info("EntityManager.contains(outbox) = {}", entityManager.contains(outbox));

    try {
      entityManager.flush();
      log.info("EntityManager.flush() 완료");
    } catch (Exception e) {
      log.error("EntityManager.flush() 실패", e);
    }

    Long count = outboxRepository.count();

    String database = jdbcTemplate.queryForObject(
        "select current_database()",
        String.class
    );

    String user = jdbcTemplate.queryForObject(
        "select current_user",
        String.class
    );

    log.info(
        "Outbox 현재 상태 - database={}, user={}, outboxCount={}",
        database,
        user,
        count
    );

    log.info("=== Outbox 저장 종료 ===");
  }

  private String writeValueAsString(NotificationRequestedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "알림 이벤트 직렬화 실패: receiverId=" + event.receiverId(),
          e
      );
    }
  }
}