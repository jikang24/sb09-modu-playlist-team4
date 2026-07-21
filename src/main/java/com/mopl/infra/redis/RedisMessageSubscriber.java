package com.mopl.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
      String body = new String(message.getBody());

      Map<String, Object> parsed = objectMapper.readValue(body, Map.class);
      String destination = (String) parsed.get("destination");
      Object payload = parsed.get("payload");

      messagingTemplate.convertAndSend("/sub/" + destination, payload);

      log.info("Redis Pub/Sub 메시지 전달 - destination: {}", destination);
    } catch (Exception e) {
      log.error("Redis Pub/Sub 메시지 처리 실패", e);
    }
  }
}