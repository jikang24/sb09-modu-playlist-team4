package com.mopl.global.config;

import com.mopl.infra.redis.RedisMessageSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

  public static final String DM_CHANNEL = "websocket:direct-messages";
  public static final String CONTENT_CHAT_CHANNEL = "websocket:content-chat";
  public static final String WATCHING_SESSION_CHANNEL = "websocket:watching-session";

  @Bean
  @ConditionalOnProperty(
      name = "redis.listener.enabled",
      havingValue = "true",
      matchIfMissing = true
  )
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisMessageSubscriber subscriber) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(subscriber, new ChannelTopic(DM_CHANNEL));
    container.addMessageListener(subscriber, new ChannelTopic(CONTENT_CHAT_CHANNEL));
    container.addMessageListener(subscriber, new ChannelTopic(WATCHING_SESSION_CHANNEL));
    return container;
  }
}