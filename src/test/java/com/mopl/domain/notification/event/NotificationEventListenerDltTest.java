package com.mopl.domain.notification.event;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.mopl.domain.notification.repository.NotificationFailureLogRepository;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.domain.user.service.AdminInitializer;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.NotificationTopics;
import com.mopl.global.security.handler.MoplOAuth2LoginSuccessHandler;
import com.mopl.infra.kafka.NotificationDeadLetterListener;
import com.mopl.infra.s3.S3Service;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    topics = {
        NotificationTopics.NOTIFICATION_REQUESTED,
        NotificationTopics.NOTIFICATION_REQUESTED + ".DLT"
    }
)
@ActiveProfiles("test")
class NotificationEventListenerDltTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private KafkaTemplate<Object, Object> kafkaTemplate;

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

  @MockitoSpyBean
  private NotificationDeadLetterListener deadLetterListener;

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

    kafkaTemplate.send(NotificationTopics.NOTIFICATION_REQUESTED, event).get();

    await()
        .pollInterval(Duration.ofMillis(500))
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(() -> {

          // DLT Listener가 실제 호출됐는지 확인
          verify(deadLetterListener, atLeastOnce())
              .handleDeadLetter(any());

          // FailureLog도 저장됐는지 확인
          org.assertj.core.api.Assertions.assertThat(
                  failureLogRepository.findAll())
              .hasSize(1);
        });
  }

  @Autowired
  ApplicationContext applicationContext;

  @Test
  void printKafkaTemplates() {
    applicationContext.getBeansOfType(KafkaTemplate.class)
        .forEach((name, bean) -> System.out.println(name + " -> " + bean));
  }

  @Autowired
  ApplicationContext context;

  @Test
  void printKafkaBeans() {
    context.getBeansOfType(KafkaTemplate.class)
        .forEach((name, bean) ->
            System.out.println(name + " : " + bean));

    context.getBeansOfType(ProducerFactory.class)
        .forEach((name, bean) ->
            System.out.println(name + " : " + bean));
  }


  @Test
  void printKafkaBean() {
    KafkaTemplate<?, ?> bean = context.getBean(KafkaTemplate.class);

    System.out.println("Bean class = " + bean.getClass());
    System.out.println("Bean = " + bean);

    String[] names = context.getBeanNamesForType(KafkaTemplate.class);
    for (String name : names) {
      System.out.println("KafkaTemplate bean = " + name);
    }
  }

  @Test
  void checkInjectedTemplate() {
    System.out.println(kafkaTemplate);

    ProducerFactory<?, ?> pf = kafkaTemplate.getProducerFactory();
    System.out.println(pf);

    System.out.println(
        pf.getConfigurationProperties().get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
    );
  }
}