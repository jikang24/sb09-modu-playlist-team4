package com.mopl.infra.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import com.mopl.global.outbox.OutboxEvent;
import com.mopl.global.outbox.OutboxRepository;
import com.mopl.global.outbox.OutboxStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaNotificationEventPublisherTest {

  @Mock
  private OutboxRepository outboxRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  private KafkaNotificationEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new KafkaNotificationEventPublisher(outboxRepository, new ObjectMapper(), jdbcTemplate);
  }

  @Test
  void publish_호출시_outbox에_topic과_eventType이_분리되어_저장된다() {
    // given
    UUID receiverId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    NotificationRequestedEvent event =
        new NotificationRequestedEvent(receiverId, "FOLLOW", "새 팔로워", "누군가 팔로우했습니다");

    // when
    publisher.publish(event);

    // then
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(captor.capture());

    OutboxEvent saved = captor.getValue();
    assertThat(saved.getTopic()).isEqualTo(NotificationTopics.NOTIFICATION_REQUESTED);
    assertThat(saved.getEventType()).isEqualTo("NOTIFICATION_REQUESTED");
    assertThat(saved.getTopic()).isNotEqualTo(saved.getEventType()); // 이전 버그 재발 방지
    assertThat(saved.getAggregateType()).isEqualTo("NOTIFICATION");
    assertThat(saved.getAggregateId()).isEqualTo(receiverId);
    assertThat(saved.getMessageKey()).isEqualTo(receiverId.toString());
    assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(saved.getPayload()).contains(receiverId.toString(), "FOLLOW");
  }
}