package com.mopl.domain.dm.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.adapter.in.web.mapper.DirectMessageWebMapper;
import com.mopl.domain.dm.adapter.in.websocket.dto.DirectMessageSendRequest;
import com.mopl.domain.dm.application.port.in.SendDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.config.RedisConfig;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessagingException;
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
  private StringRedisTemplate redisTemplate;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private NotificationEventPublisher notificationEventPublisher;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

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

  /** 성공 경로의 공통 stubbing */
  private DirectMessageDto stubSuccessPath(String content) throws Exception {
    Conversation conversation = new Conversation(conversationId, senderId, receiverId, Instant.now());
    given(getConversationUseCase.getById(conversationId, senderId)).willReturn(conversation);

    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, content);
    given(sendDirectMessageUseCase.send(conversationId, content, senderId, receiverId))
        .willReturn(directMessage);

    UserSummary senderSummary = new UserSummary(senderId, "sender", null);
    UserSummary receiverSummary = new UserSummary(receiverId, "receiver", null);
    DirectMessageDto dto = new DirectMessageDto(
        directMessage.getId(), conversationId, directMessage.getCreatedAt(),
        senderSummary, receiverSummary, content
    );
    given(directMessageWebMapper.toDto(directMessage)).willReturn(dto);

    String jsonPayload = "{\"destination\":\"conversations/" + conversationId + "/direct-messages\",\"payload\":{}}";
    given(objectMapper.writeValueAsString(any())).willReturn(jsonPayload);
    return dto;
  }

  @Test
  @DisplayName("DM 전송 성공 - 저장, 전달, 알림발행까지 정상 수행")
  void sendMessage_success() throws Exception {

    JwtClaims claims = createClaims(senderId);
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");
    stubSuccessPath("안녕하세요");

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    handler.sendMessage(conversationId, request, accessor);

    then(sendDirectMessageUseCase).should().send(conversationId, "안녕하세요", senderId, receiverId);
    then(redisTemplate).should().convertAndSend(eq(RedisConfig.DM_CHANNEL), anyString());
    then(notificationEventPublisher).should().publish(any());
  }

  @Test
  @DisplayName("DM 전송 거부 - 세션에 인증 정보(claims)가 없으면 예외 발생하고 아무것도 수행 안 됨")
  void sendMessage_fail_noClaims() {

    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
    accessor.setSessionAttributes(new HashMap<>()); // claims 없음

    assertThatThrownBy(() -> handler.sendMessage(conversationId, request, accessor))
        .isInstanceOf(MoplException.class);

    then(getConversationUseCase).should(never()).getById(any(), any());
    then(sendDirectMessageUseCase).should(never()).send(any(), any(), any(), any());
    then(notificationEventPublisher).should(never()).publish(any());
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
    then(redisTemplate).should(never()).convertAndSend(anyString(), anyString());
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

  @Test
  @DisplayName("Redis 발행이 실패해도 예외가 전파되지 않고 알림은 정상 발행된다 (DM은 이미 DB에 저장됨)")
  void sendMessage_redisPublishFails_notificationStillPublished() throws Exception {

    JwtClaims claims = createClaims(senderId);
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요");
    stubSuccessPath("안녕하세요");

    willThrow(new RuntimeException("Redis 연결 실패"))
        .given(redisTemplate).convertAndSend(eq(RedisConfig.DM_CHANNEL), anyString());

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    // 예외가 밖으로 던져지지 않아야 하고
    assertThatCode(() -> handler.sendMessage(conversationId, request, accessor))
        .doesNotThrowAnyException();

    // 실시간 팬아웃 실패와 무관하게 알림 발행은 계속되어야 한다
    then(notificationEventPublisher).should().publish(any());
  }

  @Nested
  @DisplayName("@MessageExceptionHandler 테스트")
  class MessageExceptionHandlerTest {

    private final Principal principal = () -> "test-user";

    @Test
    @DisplayName("MoplException 발생 시 보낸 사람의 개인 에러 큐로 에러 메시지를 전달한다")
    void handleMoplException_sendsErrorToUserQueue() {
      MoplException e = new MoplException(ErrorCode.CONVERSATION_NOT_FOUND);

      handler.handleMoplException(e, principal);

      then(messagingTemplate).should().convertAndSendToUser(
          eq("test-user"), eq("/queue/errors"), any(Map.class));
    }

    @Test
    @DisplayName("일반 Exception 발생 시 내부 정보 대신 일반 문구로 에러를 전달한다")
    void handleException_sendsGenericErrorMessage() {
      Exception e = new RuntimeException("DB connection pool exhausted"); // 내부 정보

      handler.handleException(e, principal);

      then(messagingTemplate).should().convertAndSendToUser(
          eq("test-user"), eq("/queue/errors"),
          eq(Map.of("error", "메시지 처리 중 오류가 발생했습니다.")));
    }

    @Test
    @DisplayName("principal이 없으면(비인증 상태 예외) 에러 전송 없이 로그만 남긴다")
    void handleException_nullPrincipal_doesNotSend() {
      assertThatCode(() -> handler.handleException(new RuntimeException("boom"), null))
          .doesNotThrowAnyException();

      then(messagingTemplate).should(never())
          .convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("에러 응답 전송 자체가 실패해도 예외를 밖으로 던지지 않는다 (마지막 방어선)")
    void sendErrorToUser_fails_doesNotThrow() {
      willThrow(new MessagingException("전송 실패"))
          .given(messagingTemplate)
          .convertAndSendToUser(anyString(), anyString(), any(Map.class));

      assertThatCode(() -> handler.handleMoplException(
          new MoplException(ErrorCode.CONVERSATION_NOT_FOUND), principal))
          .doesNotThrowAnyException();
    }
  }
}
