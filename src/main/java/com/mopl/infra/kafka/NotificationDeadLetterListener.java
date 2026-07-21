package com.mopl.infra.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import com.mopl.domain.notification.domain.NotificationFailureLog;
import com.mopl.domain.notification.repository.NotificationFailureLogRepository;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeadLetterListener {

  private final NotificationFailureLogRepository failureLogRepository;
  private final MeterRegistry meterRegistry;

  private Counter dltCounter;

  @PostConstruct
  void registerMetrics() {
    dltCounter = Counter.builder("notification.dlt.total")
        .description("DLT로 이동한 알림 이벤트 수")
        .register(meterRegistry);
  }

  @KafkaListener(topics = NotificationTopics.NOTIFICATION_REQUESTED + ".DLT", groupId = "notification-service-dlt")
  public void handleDeadLetter(NotificationRequestedEvent event) {
    try {
      log.error("Notification moved to DLT: eventId={}, type={}, receiverId={}",
          event.eventId(), event.type(), event.receiverId());
      failureLogRepository.save(NotificationFailureLog.of(event, "max retries exceeded"));
      dltCounter.increment();
    } catch (Exception e) {
      log.error("DLT 처리 자체 실패 - 수동 확인 필요. type={}, receiverId={}", event.type(), event.receiverId(), e);
    }
  }
}
