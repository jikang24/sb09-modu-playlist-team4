package com.mopl.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.mopl.domain.notification.repository.NotificationFailureLogRepository;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.domain.user.service.AdminInitializer;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import com.mopl.global.security.handler.MoplOAuth2LoginSuccessHandler;
import com.mopl.infra.s3.S3Service;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    topics = { NotificationTopics.NOTIFICATION_REQUESTED, NotificationTopics.NOTIFICATION_REQUESTED + ".DLT" }
)
@ActiveProfiles("test")
class NotificationEventListenerDltTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private KafkaTemplate<String, NotificationRequestedEvent> kafkaTemplate;

  @Autowired
  private NotificationFailureLogRepository failureLogRepository;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private S3Service s3Service;

  @MockitoBean
  private MoplOAuth2LoginSuccessHandler moplOAuth2LoginSuccessHandler;

  @MockitoBean
  private AdminInitializer adminInitializer;

  @Test
  void dbOryeoOryu_MaxRetryHoo_DltRoIdongHago_FailureLogEEEEE_Namneunda() throws Exception {
    doThrow(new RuntimeException("DB connection failed"))
        .when(notificationService)
        .send(any(), any(), any(), any(), any());

    NotificationRequestedEvent event = new NotificationRequestedEvent(
        UUID.randomUUID(),
        "FOLLOW",
        "title",
        "content"
    );

    // Kafka에 실제 전송 완료까지 대기
    kafkaTemplate.send(NotificationTopics.NOTIFICATION_REQUESTED, event).get();

    await()
        .pollInterval(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(() -> {
          assertThat(failureLogRepository.findAll()).hasSize(1);
        });
  }
}