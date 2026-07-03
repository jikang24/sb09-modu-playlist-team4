package com.mopl.infra.kafka;

import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

  private final KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

  @Override
  public void publish(NotificationRequestedEvent event) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          send(event);
        }
      });
      return;
    }

    send(event);
  }

  private void send(NotificationRequestedEvent event) {
    kafkaTemplate.send(
        NotificationTopics.NOTIFICATION_REQUESTED,
        event.receiverId().toString(),
        event
    );
  }
}
