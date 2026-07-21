package com.mopl.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.mopl.global.event.NotificationRequestedEvent;

@Entity
@Table(name = "notification_failure_logs")
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationFailureLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(columnDefinition = "uuid")
  private UUID eventId;

  @Column(columnDefinition = "uuid")
  private UUID receiverId;

  @Column(length = 50)
  private String type;

  @Column(columnDefinition = "text")
  private String title;

  @Column(columnDefinition = "text")
  private String content;

  @Column(columnDefinition = "text")
  private String errorMessage;

  @CreatedDate
  @Column(nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
  private Instant occurredAt;

  public static NotificationFailureLog of(NotificationRequestedEvent event, String errorMessage) {
    return NotificationFailureLog.builder()
        .eventId(event.eventId())
        .receiverId(event.receiverId())
        .type(event.type())
        .title(event.title())
        .content(event.content())
        .errorMessage(errorMessage)
        .build();
  }
}