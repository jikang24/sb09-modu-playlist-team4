package com.mopl.domain.dm.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.adapter.in.web.mapper.DirectMessageWebMapper;
import com.mopl.domain.dm.adapter.in.websocket.dto.DirectMessageSendRequest;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebSocketHandlerTest {

  @InjectMocks
  private DirectMessageWebSocketHandler handler;

  @Mock
  private SendDirectMessageUseCase sendDirectMessageUseCase;

  @Mock
  private GetConversationUseCase getConversationUseCase;

  @Mock
  private DirectMessageWebMapper directMessageWebMapper;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private NotificationEventPublisher notificationEventPublisher;

  private final UUID senderId = UUID.randomUUID();
  private final UUID receiverId = UUID.randomUUID();
  private final UUID conversationId = UUID.randomUUID();

  private SimpMessageHeaderAccessor createAccessorWithClaims(JwtClaims claims) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
    accessor.setSessionAttributes(new HashMap<>());
    accessor.getSessionAttributes().put("claims", claims);
    return accessor;
  }

  private JwtClaims createClaims(UUID userId) {
    return JwtClaims.builder()
        .userId(userId)
        .email("test@email.com")
        .role("USER")
        .tokenId(UUID.randomUUID().toString())
        .build();
  }

  @Test
  @DisplayName("DM 전송 성공 - 저장, 전달, 알림발행까지 정상 수행")
  void sendMessage_success() {
    
    JwtClaims claims = createClaims(senderId);
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");

    Conversation conversation = new Conversation(conversationId, senderId, receiverId, Instant.now());
    given(getConversationUseCase.getById(conversationId, senderId)).willReturn(conversation);

    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕하세요");
    given(sendDirectMessageUseCase.send(conversationId, "안녕하세요", senderId, receiverId))
        .willReturn(directMessage);

    UserSummary senderSummary = new UserSummary(senderId, "sender", null);
    UserSummary receiverSummary = new UserSummary(receiverId, "receiver", null);
    DirectMessageDto dto = new DirectMessageDto(
        directMessage.getId(), conversationId, directMessage.getCreatedAt(),
        senderSummary, receiverSummary, "안녕하세요"
    );
    given(directMessageWebMapper.toDto(directMessage)).willReturn(dto);

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    
    handler.sendMessage(conversationId, request, accessor);

    
    then(sendDirectMessageUseCase).should().send(conversationId, "안녕하세요", senderId, receiverId);
    then(messagingTemplate).should().convertAndSend(
        "/sub/conversations/" + conversationId + "/direct-messages", dto
    );
    then(notificationEventPublisher).should().publish(any());
  }

  @Test
  @DisplayName("DM 전송 실패 - 참여자가 아닌 대화방이면 예외 발생하고 저장/전달 안 됨")
  void sendMessage_fail_notParticipant() {
    
    JwtClaims claims = createClaims(senderId);
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");

    given(getConversationUseCase.getById(conversationId, senderId))
        .willThrow(new MoplException(ErrorCode.FORBIDDEN_ACCESS));

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    
    assertThatThrownBy(() -> handler.sendMessage(conversationId, request, accessor))
        .isInstanceOf(MoplException.class);

    then(sendDirectMessageUseCase).should(never()).send(any(), any(), any(), any());
    then(messagingTemplate).should(never()).convertAndSend(anyString(), any(Object.class));
    then(notificationEventPublisher).should(never()).publish(any());
  }

  @Test
  @DisplayName("DM 전송 실패 - 존재하지 않는 대화방이면 예외 발생")
  void sendMessage_fail_conversationNotFound() {
    
    JwtClaims claims = createClaims(senderId);
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");

    given(getConversationUseCase.getById(conversationId, senderId))
        .willThrow(new MoplException(ErrorCode.CONVERSATION_NOT_FOUND));

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    
    assertThatThrownBy(() -> handler.sendMessage(conversationId, request, accessor))
        .isInstanceOf(MoplException.class);

    then(sendDirectMessageUseCase).should(never()).send(any(), any(), any(), any());
  }
}