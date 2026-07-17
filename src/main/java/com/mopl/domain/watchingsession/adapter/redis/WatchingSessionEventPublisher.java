package com.mopl.domain.watchingsession.adapter.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.watchingsession.application.port.out.PublishWatchingSessionEventPort;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange;
import com.mopl.global.config.RedisConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * [Adapter Out] 고정 채널(WATCHING_SESSION_CHANNEL) 하나에 destination/payload를 담은 봉투를 발행하는 방식.
 * ElastiCache Serverless가 PSUBSCRIBE를 지원하지 않아 채널을 동적으로 나누지 않고 이 방식을 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventPublisher implements PublishWatchingSessionEventPort {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void publish(UUID contentId, WatchingSessionChange change) {
    try {
      Map<String, Object> message = new HashMap<>();
      message.put("destination", "contents/" + contentId + "/watch");
      message.put("payload", change);

      String jsonPayload = objectMapper.writeValueAsString(message);
      redisTemplate.convertAndSend(RedisConfig.WATCHING_SESSION_CHANNEL, jsonPayload);
      log.info("Redis 발행 완료 - contentId: {}", contentId);
    } catch (Exception e) {
      log.error("Redis 발행 실패 - contentId: {}", contentId, e);
    }
  }
}