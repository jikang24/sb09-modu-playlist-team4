package com.mopl.infra.kafka;

import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaNotificationEventPublisher 테스트")
class KafkaNotificationEventPublisherTest {

  @Mock
  private KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

  private KafkaNotificationEventPublisher publisher;

  private NotificationRequestedEvent event;
  private UUID receiverId;

  @BeforeEach
  void setUp() {
    publisher = new KafkaNotificationEventPublisher(kafkaTemplate);
    receiverId = UUID.randomUUID();
    event = new NotificationRequestedEvent(
        receiverId,
        "FOLLOW",
        "새 팔로워",
        "누군가 팔로우했습니다."
    );
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clear();
    }
  }

  @Test
  @DisplayName("성공: 트랜잭션이 없으면 즉시 Kafka로 발행한다")
  void publish_withoutTransaction() {
    publisher.publish(event);

    verify(kafkaTemplate).send(
        NotificationTopics.NOTIFICATION_REQUESTED,
        receiverId.toString(),
        event
    );
  }

  @Test
  @DisplayName("성공: 트랜잭션 커밋 후 Kafka로 발행한다")
  void publish_afterTransactionCommit() {
    TransactionSynchronizationManager.initSynchronization();

    publisher.publish(event);
    verify(kafkaTemplate, never()).send(
        eq(NotificationTopics.NOTIFICATION_REQUESTED),
        eq(receiverId.toString()),
        eq(event)
    );

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCommit());

    ArgumentCaptor<NotificationRequestedEvent> eventCaptor =
        ArgumentCaptor.forClass(NotificationRequestedEvent.class);
    verify(kafkaTemplate).send(
        eq(NotificationTopics.NOTIFICATION_REQUESTED),
        eq(receiverId.toString()),
        eventCaptor.capture()
    );
    assertThat(eventCaptor.getValue()).isEqualTo(event);
  }
}
