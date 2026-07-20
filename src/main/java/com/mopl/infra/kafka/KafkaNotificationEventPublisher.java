package com.mopl.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import com.mopl.global.outbox.OutboxEvent;
import com.mopl.global.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationEventPublisher 의 카프카 어댑터.
 * 카프카로 직접 보내지 않고 outbox_event 테이블에 기록만 한다.
 * 이 메서드는 알림을 유발한 도메인 저장과 "같은 트랜잭션" 안에서 호출되어야 한다
 * (@Transactional propagation 기본값 REQUIRED이므로, 호출자가 이미 트랜잭션 중이면 그 트랜잭션에 참여한다).
 */
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void publish(NotificationRequestedEvent event) {
    String receiverId = event.receiverId().toString();

    OutboxEvent outbox = OutboxEvent.builder()
        .aggregateType("NOTIFICATION")
        .aggregateId(event.receiverId())
        .eventType("NOTIFICATION_REQUESTED")               // 이벤트의 의미
        .topic(NotificationTopics.NOTIFICATION_REQUESTED)  // 실제 발행될 카프카 토픽
        .messageKey(receiverId)
        .payload(writeValueAsString(event))
        .build();

    outboxRepository.save(outbox);
  }

  private String writeValueAsString(NotificationRequestedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "알림 이벤트 직렬화 실패: receiverId=" + event.receiverId(), e);
    }
  }
}