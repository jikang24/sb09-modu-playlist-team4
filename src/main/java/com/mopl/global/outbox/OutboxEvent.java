package com.mopl.global.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

  @Id
  private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  /**
   * 이벤트의 의미(어떤 일이 일어났는지). 예: "NOTIFICATION_REQUESTED"
   */
  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  /**
   * 실제로 발행될 카프카 토픽명. eventType과 값이 같을 수도 있지만 의미상 분리해서 관리한다.
   */
  @Column(name = "topic", nullable = false, length = 150)
  private String topic;

  @Column(name = "message_key", length = 100)
  private String messageKey;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OutboxStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Builder
  public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType,
      String topic, String messageKey, String payload, OutboxStatus status) {
    this.id = (id != null) ? id : UUID.randomUUID();
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.topic = topic;
    this.messageKey = (messageKey != null) ? messageKey : aggregateId.toString();
    this.payload = payload;
    this.status = (status != null) ? status : OutboxStatus.PENDING;
    this.retryCount = 0;
    this.createdAt = LocalDateTime.now();
  }

  public void markPublished() {
    this.status = OutboxStatus.PUBLISHED;
    this.publishedAt = LocalDateTime.now();
  }

  public void markFailed(int maxRetry) {
    this.retryCount++;
    if (this.retryCount >= maxRetry) {
      this.status = OutboxStatus.FAILED;
    }
  }
}