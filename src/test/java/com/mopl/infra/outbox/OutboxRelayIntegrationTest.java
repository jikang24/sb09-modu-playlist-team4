package com.mopl.infra.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mopl.global.outbox.OutboxEvent;
import com.mopl.global.outbox.OutboxRepository;
import com.mopl.global.outbox.OutboxStatus;
import com.mopl.global.event.NotificationTopics;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = NotificationTopics.NOTIFICATION_REQUESTED)
@TestPropertySource(properties = {
    "opensearch.init.enabled=false",
    "redis.listener.enabled=false",
    "spring.batch.job.enabled=false",
    "outbox.relay.fixed-delay-ms=200",
    "kafka.topic.replication-factor=1",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "MAIL_USERNAME=test@test.com",
    "MAIL_PASSWORD=test"

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

  @MockitoBean
  private com.mopl.infra.s3.S3Service s3Service;

  private UUID savedId;

  @AfterEach
  void tearDown() {
    if (savedId != null) {
      outboxRepository.deleteById(savedId);
    }
  }

  @Test
  void PENDING_이벤트는_스케줄러에_의해_자동으로_PUBLISHED된다() {
    UUID receiverId = UUID.randomUUID();
    OutboxEvent event = OutboxEvent.builder()
        .aggregateType("NOTIFICATION")
        .aggregateId(receiverId)
        .eventType("NOTIFICATION_REQUESTED")
        .topic(NotificationTopics.NOTIFICATION_REQUESTED)
        .messageKey(receiverId.toString())
        .payload("{\"eventId\":\"" + UUID.randomUUID() + "\","
            + "\"receiverId\":\"" + receiverId + "\","
            + "\"type\":\"FOLLOW\","
            + "\"title\":\"새 팔로워\","
            + "\"content\":\"누군가 팔로우했습니다.\"}")
        .build();

    savedId = outboxRepository.saveAndFlush(event).getId();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      OutboxEvent reloaded = outboxRepository.findById(savedId).orElseThrow();

      assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
      assertThat(reloaded.getPublishedAt()).isNotNull();
    });
  }
}
