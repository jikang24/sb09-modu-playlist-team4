package com.mopl.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.sse.SseNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer가 DB에 저장한 알림을 Redis Pub/Sub으로 fanout하고,
 * 각 인스턴스는 로컬 SSE emitter가 있을 때만 클라이언트에 전달한다.
 * (DM/ContentChat/ViewingSession과 동일한 멀티 인스턴스 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

  private final SseNotificationSender sseNotificationSender;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      NotificationDto notification =
          objectMapper.readValue(message.getBody(), NotificationDto.class);
      sseNotificationSender.send(notification.receiverId(), notification);
      log.debug("Redis 알림 SSE fanout - receiverId={}, notificationId={}",
          notification.receiverId(), notification.id());
    } catch (Exception e) {
      log.error("Redis 알림 SSE fanout 처리 실패", e);
    }
  }
}
