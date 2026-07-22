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
      log.info("알림 요청 수신 - eventId={}, type={}, receiverId={}",
          event.eventId(), event.type(), event.receiverId());
      notificationService.send(
          event.eventId(),
          event.receiverId(),
          NotificationType.valueOf(event.type()),
          event.title(),
          event.content()
      );
      log.info("NotificationService.send 완료 - eventId={}, receiverId={}, type={}",
          event.eventId(), event.receiverId(), event.type());
    } catch (IllegalArgumentException e) {
      // 복구 불가능한 데이터 오류 -> 재시도 의미 없음, 여기서 삼키고 종료 (에러 핸들러까지 안 감)
      log.warn("[{}] Unknown notification type: type={}, receiverId={}",
          ErrorCode.INVALID_NOTIFICATION_TYPE.name(), event.type(), event.receiverId());
    } catch (Exception e) {
      // DB 오류 등 그 외 모든 예외 -> 로그 남기고 재던져서 DefaultErrorHandler(재시도+DLQ)로 위임
      log.error("Notification processing failed: type={}, receiverId={}",
          event.type(), event.receiverId(), e);
      throw e;
    }
  }
}
