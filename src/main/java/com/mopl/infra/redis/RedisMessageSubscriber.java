package com.mopl.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String channel = new String(message.getChannel());
      String body = new String(message.getBody());

      String destination = channel.substring("websocket:".length());

      messagingTemplate.convertAndSend("/sub/" + destination, body);

      log.info("Redis Pub/Sub 메시지 전달 - channel: {}", channel);
    } catch (Exception e) {
      log.error("Redis Pub/Sub 메시지 처리 실패", e);
    }
  }
}