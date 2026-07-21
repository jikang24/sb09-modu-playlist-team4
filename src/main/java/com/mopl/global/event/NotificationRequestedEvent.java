package com.mopl.global.event;

import java.util.UUID;

public record NotificationRequestedEvent(
    UUID eventId,
    UUID receiverId,
    String type,
    String title,
    String content
) {

  /** Outbox 발행 시점에 eventId가 자동 부여된다. 기존 호출부 호환용 생성자. */
  public NotificationRequestedEvent(UUID receiverId, String type, String title, String content) {
    this(UUID.randomUUID(), receiverId, type, title, content);
  }
}
