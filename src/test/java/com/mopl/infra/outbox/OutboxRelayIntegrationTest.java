package com.mopl.infra.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mopl.global.outbox.OutboxEvent;
import com.mopl.global.outbox.OutboxRepository;
import com.mopl.global.outbox.OutboxStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "notification-requested")
@TestPropertySource(properties = {
    "opensearch.init.enabled=false",
    "redis.listener.enabled=false",
    "outbox.relay.fixed-delay-ms=200",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",

})
class OutboxRelayIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("mopl")
          .withUsername("postgres")
          .withPassword("postgres");

  @Autowired
  private OutboxRepository outboxRepository;

  @MockitoBean
  private JobLauncher jobLauncher;

  private UUID savedId;

  @AfterEach
  void tearDown() {
    if (savedId != null) {
      outboxRepository.deleteById(savedId);
    }
  }

  @Test
  void PENDING_이벤트는_스케줄러에_의해_자동으로_PUBLISHED된다() {
    OutboxEvent event = OutboxEvent.builder()
        .aggregateType("NOTIFICATION")
        .aggregateId(UUID.randomUUID())
        .eventType("NOTIFICATION_REQUESTED")
        .topic("notification-requested")
        .messageKey("user-1")
        .payload("{\"receiverId\":\"user-1\"}")
        .build();

    savedId = outboxRepository.saveAndFlush(event).getId();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      OutboxEvent reloaded = outboxRepository.findById(savedId).orElseThrow();

      assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
      assertThat(reloaded.getPublishedAt()).isNotNull();
    });
  }
}