package com.mopl.domain.contentchat.adapter.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.contentchat.adapter.in.websocket.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.adapter.port.LoadUserPort;
import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtClaims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * 콘텐츠(같이보기) 채팅 - DB 저장 없이 구독자에게 그대로 릴레이만 함 (요구사항)
 * 서버가 여러 대일 때도 모든 구독자에게 전달되도록 Redis pub/sub을 거쳐 릴레이한다
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ContentChatWebSocketHandler {

  private final LoadUserPort loadUserPort;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @MessageMapping("/contents/{contentId}/chat")
  public void sendMessage(
      @DestinationVariable UUID contentId,
      ContentChatSendRequest request,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    JwtClaims claims = (JwtClaims) headerAccessor.getSessionAttributes().get("claims");
    UUID senderId = claims.getUserId();
    UserSummary sender = loadUserPort.getUserSummary(senderId);

    ContentChatDto dto = new ContentChatDto(sender, request.content());

    try {
      String jsonPayload = objectMapper.writeValueAsString(dto);
      String channel = "websocket:contents/" + contentId + "/chat";
      redisTemplate.convertAndSend(channel, jsonPayload);
    } catch (Exception e) {
      log.error("Redis 발행 실패 - contentId: {}", contentId, e);
    }
    log.info("Redis 발행 완료 - contentId: {}", contentId);
    log.info("콘텐츠 채팅 전달 완료 - contentId: {}, senderId: {}", contentId, senderId);
  }
}