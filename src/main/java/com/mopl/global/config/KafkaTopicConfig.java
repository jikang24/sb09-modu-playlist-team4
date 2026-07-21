package com.mopl.global.config;

import com.mopl.global.event.NotificationTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic notificationRequestedTopic() {
    return TopicBuilder.name(NotificationTopics.NOTIFICATION_REQUESTED)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic notificationRequestedDlt() {
    return TopicBuilder.name(NotificationTopics.NOTIFICATION_REQUESTED + ".DLT")
        .partitions(3)
        .replicas(1)
        .build();
  }
}