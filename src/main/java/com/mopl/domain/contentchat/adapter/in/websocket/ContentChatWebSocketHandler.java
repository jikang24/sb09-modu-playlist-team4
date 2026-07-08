package com.mopl.domain.contentchat.adapter.in.websocket;

import com.mopl.domain.contentchat.adapter.in.websocket.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.adapter.port.LoadUserPort;
import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtClaims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 콘텐츠(같이보기) 채팅 - DB 저장 없이 구독자에게 그대로 릴레이만 함 (요구사항)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ContentChatWebSocketHandler {

  private final LoadUserPort loadUserPort;
  private final SimpMessagingTemplate messagingTemplate;

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

    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", dto);
    log.info("콘텐츠 채팅 전달 완료 - contentId: {}, senderId: {}", contentId, senderId);
  }
}