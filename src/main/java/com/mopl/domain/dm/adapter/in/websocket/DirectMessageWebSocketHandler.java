package com.mopl.domain.dm.adapter.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.dm.adapter.in.websocket.dto.DirectMessageSendRequest;
import com.mopl.domain.dm.adapter.in.web.mapper.DirectMessageWebMapper;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.config.RedisConfig;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.jwt.JwtClaims;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DirectMessageWebSocketHandler {

  private final SendDirectMessageUseCase sendDirectMessageUseCase;
  private final GetConversationUseCase getConversationUseCase;
  private final DirectMessageWebMapper directMessageWebMapper;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final NotificationEventPublisher notificationEventPublisher;

  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void sendMessage(
      @DestinationVariable UUID conversationId,
      DirectMessageSendRequest request,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    JwtClaims jwtClaims = (JwtClaims) headerAccessor.getSessionAttributes().get("claims");
    UUID senderId = jwtClaims.getUserId();
    log.info("WebSocket DM 전송 요청 - senderId: {}, conversationId: {}", senderId, conversationId);

    var conversation = getConversationUseCase.getById(conversationId, senderId);
    UUID receiverId = conversation.getOtherParticipant(senderId);

    DirectMessage directMessage = sendDirectMessageUseCase.send(
        conversationId, request.content(), senderId, receiverId
    );

    log.info("WebSocket DM 저장 완료 - directMessageId: {}", directMessage.getId());

    DirectMessageDto dto = directMessageWebMapper.toDto(directMessage);

    try {
      Map<String, Object> message = new HashMap<>();
      message.put("destination", "conversations/" + conversationId + "/direct-messages");
      message.put("payload", dto);

      String jsonPayload = objectMapper.writeValueAsString(message);
      redisTemplate.convertAndSend(RedisConfig.DM_CHANNEL, jsonPayload);
    } catch (Exception e) {
      log.error("Redis 발행 실패 - conversationId: {}", conversationId, e);
    }


    log.info("Redis 발행 완료 - conversationId: {}", conversationId);

    notificationEventPublisher.publish(new NotificationRequestedEvent(
        receiverId,
        "DIRECT_MESSAGE",
        "[DM]" + " " + dto.sender().name(),
        request.content()
    ));
  }
}