package com.mopl.infra.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.global.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationPublisher {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public void publish(NotificationDto notification) {
    try {
      String json = objectMapper.writeValueAsString(notification);
      redisTemplate.convertAndSend(RedisConfig.NOTIFICATION_CHANNEL, json);
    } catch (JsonProcessingException e) {
      log.error("알림 SSE fanout 발행 실패: notificationId={}", notification.id(), e);
    }
  }
}
