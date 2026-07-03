package com.mopl.domain.dm.adapter.in.websocket;

import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.dm.adapter.in.websocket.dto.DirectMessageSendRequest;
import com.mopl.domain.dm.adapter.in.web.mapper.DirectMessageWebMapper;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.jwt.JwtClaims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DirectMessageWebSocketHandler {

  private final SendDirectMessageUseCase sendDirectMessageUseCase;
  private final GetConversationUseCase getConversationUseCase;
  private final DirectMessageWebMapper directMessageWebMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void sendMessage(
      @DestinationVariable UUID conversationId,
      DirectMessageSendRequest request,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID senderId = claims.getUserId();
    log.info("WebSocket DM 전송 요청 - senderId: {}, conversationId: {}", senderId, conversationId);

    // 1. 대화방 조회 + 참여자 검증
    var conversation = getConversationUseCase.getById(conversationId, senderId);

    // 2. 수신자 ID 구하기
    UUID receiverId = conversation.getOtherParticipant(senderId);

    // 3. DM 저장
    DirectMessage directMessage = sendDirectMessageUseCase.send(
        conversationId, request.content(), senderId, receiverId
    );

    log.info("WebSocket DM 저장 완료 - directMessageId: {}", directMessage.getId());

    // 4. DTO로 변환
    DirectMessageDto dto = directMessageWebMapper.toDto(directMessage);

    // 5. 구독 중인 클라이언트에게 전달
    messagingTemplate.convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages",
        dto
    );

    log.info("WebSocket DM 전달 완료 - conversationId: {}", conversationId);
  }
}