package com.mopl.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

  private final OutboxRepository repository;
  private final KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;
  private final ObjectMapper objectMapper;

}
