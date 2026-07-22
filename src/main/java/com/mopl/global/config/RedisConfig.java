package com.mopl.global.config;

import java.time.Duration;

import com.mopl.infra.redis.RedisMessageSubscriber;
import com.mopl.infra.redis.RedisNotificationSubscriber;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
public class RedisConfig {

  public static final String DM_CHANNEL = "websocket:direct-messages";
  public static final String CONTENT_CHAT_CHANNEL = "websocket:content-chat";
  public static final String WATCHING_SESSION_CHANNEL = "websocket:watching-session";
  public static final String NOTIFICATION_CHANNEL = "sse:notifications";

  @Bean
  public RedisConnectionFactory listenerRedisConnectionFactory(
      RedisProperties redisProperties,
      @Value("${redis.listener.timeout:3000ms}") Duration listenerTimeout) {

    RedisStandaloneConfiguration standaloneConfig =
        new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
    if (redisProperties.getPassword() != null) {
      standaloneConfig.setPassword(redisProperties.getPassword());
    }

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
        LettuceClientConfiguration.builder().commandTimeout(listenerTimeout);
    if (redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled()) {
      clientConfigBuilder.useSsl();
    }

    return new LettuceConnectionFactory(standaloneConfig, clientConfigBuilder.build());
  }

  @Bean
  @ConditionalOnProperty(
      name = "redis.listener.enabled",
      havingValue = "true",
      matchIfMissing = true
  )
  public RedisMessageListenerContainer redisMessageListenerContainer(
      @Qualifier("listenerRedisConnectionFactory") RedisConnectionFactory listenerRedisConnectionFactory,
      RedisMessageSubscriber subscriber,
      RedisNotificationSubscriber notificationSubscriber) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(listenerRedisConnectionFactory);
    container.addMessageListener(subscriber, new ChannelTopic(DM_CHANNEL));
    container.addMessageListener(subscriber, new ChannelTopic(CONTENT_CHAT_CHANNEL));
    container.addMessageListener(subscriber, new ChannelTopic(WATCHING_SESSION_CHANNEL));
    container.addMessageListener(notificationSubscriber, new ChannelTopic(NOTIFICATION_CHANNEL));
    // 개별 메시지 처리 실패(RedisMessageSubscriber.onMessage 내부)는 이미 try-catch로 방어돼 있지만,
    // 구독/커넥션 자체가 끊기는 등 컨테이너 레벨 오류는 잡아주는 곳이 없어 조용히 묻힐 수 있었음.
    // 이 핸들러를 등록해두면 그런 상황도 최소한 로그로 남아 장애를 감지할 수 있다.
    container.setErrorHandler(throwable ->
        log.error("Redis Pub/Sub 구독 컨테이너 오류 발생", throwable));
    return container;
  }
}