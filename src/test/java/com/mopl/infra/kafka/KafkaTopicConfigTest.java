package com.mopl.infra.kafka;

import com.mopl.global.event.NotificationTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KafkaTopicConfig 테스트")
class KafkaTopicConfigTest {

  @Test
  @DisplayName("성공: notification-requested 토픽을 생성한다")
  void notificationRequestedTopic() {
    KafkaTopicConfig config = new KafkaTopicConfig();

    NewTopic topic = config.notificationRequestedTopic();

    assertThat(topic.name()).isEqualTo(NotificationTopics.NOTIFICATION_REQUESTED);
    assertThat(topic.numPartitions()).isEqualTo(3);
    assertThat(topic.replicationFactor()).isEqualTo((short) 1);
  }
}
