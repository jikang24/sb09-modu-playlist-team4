package com.mopl.global.config;

import com.mopl.infra.redis.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

  public static final String WEBSOCKET_CHANNEL_PATTERN = "websocket:*";

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter listenerAdapter) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, new PatternTopic(WEBSOCKET_CHANNEL_PATTERN));
    return container;
  }

  @Bean
  public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "onMessage");
  }
}