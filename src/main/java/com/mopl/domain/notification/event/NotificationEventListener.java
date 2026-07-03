package com.mopl.domain.notification.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @KafkaListener(
      topics = NotificationTopics.NOTIFICATION_REQUESTED,
      groupId = "notification-service"
  )
  public void handle(NotificationRequestedEvent event) {
    try {
      notificationService.send(
          event.receiverId(),
          NotificationType.valueOf(event.type()),
          event.title(),
          event.content()
      );
    } catch (IllegalArgumentException e) {
      log.warn("[{}] Unknown notification type: type={}, receiverId={}",
          ErrorCode.INVALID_NOTIFICATION_TYPE.name(), event.type(), event.receiverId());
    }
  }
}
