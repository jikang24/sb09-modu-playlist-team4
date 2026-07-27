package com.mopl.global.config;

import com.mopl.global.event.NotificationTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Value("${kafka.topic.replication-factor:3}")
  private short replicationFactor;

  @Bean
  public NewTopic notificationRequestedTopic() {
    return TopicBuilder.name(NotificationTopics.NOTIFICATION_REQUESTED)
        .partitions(6)
        .replicas(replicationFactor)
        .build();
  }

  @Bean
  public NewTopic notificationRequestedDlt() {
    return TopicBuilder.name(NotificationTopics.NOTIFICATION_REQUESTED + ".DLT")
        .partitions(6)
        .replicas(replicationFactor)
        .build();
  }
}
