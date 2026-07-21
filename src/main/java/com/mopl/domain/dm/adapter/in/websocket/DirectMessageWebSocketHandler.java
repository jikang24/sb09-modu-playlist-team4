package com.mopl.domain.dm.adapter.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.dm.adapter.in.websocket.dto.DirectMessageSendRequest;
import com.mopl.domain.dm.adapter.in.web.mapper.DirectMessageWebMapper;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.domain.notification.sse.SseNotificationSender;
import com.mopl.global.config.RedisConfig;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
  private final SimpMessagingTemplate messagingTemplate;
  private final SseNotificationSender sseNotificationSender;

  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void sendMessage(
      @DestinationVariable UUID conversationId,
      DirectMessageSendRequest request,
      SimpMessageHeaderAccessor headerAccessor
  ) {
    JwtClaims jwtClaims = (JwtClaims) headerAccessor.getSessionAttributes().get("claims");
    if (jwtClaims == null) {
      log.warn("WebSocket DM 전송 거부 - 인증 정보 없음, conversationId: {}", conversationId);
      throw new MoplException(ErrorCode.TOKEN_NOT_FOUND);
    }
    UUID senderId = jwtClaims.getUserId();
    log.info("WebSocket DM 전송 요청 - senderId: {}, conversationId: {}", senderId, conversationId);

    var conversation = getConversationUseCase.getById(conversationId, senderId);
    UUID receiverId = conversation.getOtherParticipant(senderId);

    DirectMessage directMessage = sendDirectMessageUseCase.send(
        conversationId, request.content(), senderId, receiverId
    );

    log.info("WebSocket DM 저장 완료 - directMessageId: {}", directMessage.getId());

    DirectMessageDto dto = directMessageWebMapper.toDto(directMessage);

    // 프론트: 대화 목록 화면은 항상 SSE "direct-messages"를 구독해 목록/안읽음 배지를 갱신하고
    // (읽음 여부는 프론트가 현재 선택된 대화ID와 비교해서 직접 판단),
    // 대화 상세 화면은 그 대화방을 열어보는 동안만 웹소켓을 구독해 채팅창에 실시간 렌더링한다.
    // 즉 "활성/비활성"에 따른 양자택일이 아니라 매 DM마다 둘 다 보내야 한다 - 서로 독립적인 실패 처리.
    try {
      Map<String, Object> message = new HashMap<>();
      message.put("destination", "conversations/" + conversationId + "/direct-messages");
      message.put("payload", dto);

      String jsonPayload = objectMapper.writeValueAsString(message);
      redisTemplate.convertAndSend(RedisConfig.DM_CHANNEL, jsonPayload);
      log.info("Redis 발행 완료 - conversationId: {}", conversationId);
    } catch (Exception e) {
      log.error("Redis 발행 실패 - conversationId: {}", conversationId, e);
    }

    try {
      sseNotificationSender.sendDirectMessage(receiverId, dto);
      log.info("SSE direct-messages 발행 완료 - conversationId: {}, receiverId: {}",
          conversationId, receiverId);
    } catch (Exception e) {
      log.error("SSE direct-messages 발행 실패 - conversationId: {}, receiverId: {}",
          conversationId, receiverId, e);
    }

    notificationEventPublisher.publish(new NotificationRequestedEvent(
        receiverId,
        "DIRECT_MESSAGE",
        "[DM]" + " " + dto.sender().name(),
        request.content()
    ));
  }


  @MessageExceptionHandler(MoplException.class)
  public void handleMoplException(MoplException e, Principal principal) {
    log.warn("WebSocket DM 처리 실패 - user: {}, error: {}",
        principal != null ? principal.getName() : "unknown", e.getMessage());
    sendErrorToUser(principal, e.getMessage());
  }

  @MessageExceptionHandler(Exception.class)
  public void handleException(Exception e, Principal principal) {
    log.error("WebSocket DM 처리 중 예상하지 못한 오류 - user: {}",
        principal != null ? principal.getName() : "unknown", e);
    sendErrorToUser(principal, "메시지 처리 중 오류가 발생했습니다.");
  }

  private void sendErrorToUser(Principal principal, String errorMessage) {
    if (principal == null) {
      return;
    }
    try {
      messagingTemplate.convertAndSendToUser(
          principal.getName(), "/queue/errors", Map.of("error", errorMessage));
    } catch (Exception e) {
      log.error("WebSocket 에러 응답 전송 실패", e);
    }
  }
}
