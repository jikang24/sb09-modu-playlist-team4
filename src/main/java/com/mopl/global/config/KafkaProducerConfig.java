package com.mopl.global.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Spring Boot가 기본으로 만들어주던 KafkaTemplate<Object,Object>(JsonSerializer)를
 * 우리가 outboxKafkaTemplate 빈을 직접 정의하는 순간 @ConditionalOnMissingBean(KafkaTemplate.class)
 * 때문에 더 이상 자동 생성되지 않는다. 그래서 기본 템플릿도 여기서 명시적으로 재정의해준다.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

  private final KafkaProperties kafkaProperties;

  /**
   * 기존에 Spring Boot가 자동으로 만들어주던 것과 동일한 역할.
   * application.yml의 spring.kafka.producer 설정(JsonSerializer 등)을 그대로 사용한다.
   * DeadLetterPublishingRecoverer 등 특정 타입을 지정하지 않는 모든 곳에서 기본으로 쓰인다.
   */
  @Bean
  @Primary
  public ProducerFactory<Object, Object> producerFactory() {
    Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  @Primary
  public KafkaTemplate<Object, Object> kafkaTemplate(
      ProducerFactory<Object, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  /**
   * Outbox payload는 이미 ObjectMapper로 직렬화가 끝난 순수 JSON 문자열이다.
   * 기본 템플릿(JsonSerializer)으로 이 문자열을 다시 보내면 이중 인코딩되므로,
   * Outbox 릴레이 전용으로 StringSerializer를 쓰는 템플릿을 별도로 둔다.
   */
  @Bean
  public ProducerFactory<String, String> outboxProducerFactory() {
    Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, String> outboxKafkaTemplate(
      ProducerFactory<String, String> outboxProducerFactory) {
    return new KafkaTemplate<>(outboxProducerFactory);
  }
}