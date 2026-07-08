package com.mopl.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

  @InjectMocks
  private StompAuthInterceptor stompAuthInterceptor;

  @Mock
  private JwtProvider jwtProvider;

  @Mock
  private GetConversationUseCase getConversationUseCase;

  @Mock
  private MessageChannel messageChannel;

  private final UUID myId = UUID.randomUUID();
  private final UUID otherId = UUID.randomUUID();

  private Message<?> createConnectMessage(String token) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionAttributes(new HashMap<>());
    if (token != null) {
      accessor.setNativeHeader("Authorization", "Bearer " + token);
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<?> createSubscribeMessage(String destination, JwtClaims claims) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setSessionAttributes(new HashMap<>());
    accessor.setDestination(destination);
    accessor.getSessionAttributes().put("claims", claims);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("CONNECT 성공 - 토큰 유효하면 claims가 세션에 저장된다")
  void preSend_connect_success() {
    
    String token = "valid-token";
    JwtClaims claims = JwtClaims.builder()
        .userId(myId)
        .email("test@email.com")
        .role("USER")
        .tokenId(UUID.randomUUID().toString())
        .build();

    given(jwtProvider.parse(token)).willReturn(claims);

    Message<?> message = createConnectMessage(token);

    
    Message<?> result = stompAuthInterceptor.preSend(message, messageChannel);

    
    assertThat(result).isNotNull();
    StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
    assertThat(resultAccessor.getSessionAttributes().get("claims")).isEqualTo(claims);
  }

  @Test
  @DisplayName("CONNECT 실패 - Authorization 헤더가 없으면 MessagingException 발생")
  void preSend_connect_fail_noToken() {

    Message<?> message = createConnectMessage(null);


    assertThatThrownBy(() -> stompAuthInterceptor.preSend(message, messageChannel))
        .isInstanceOf(MessagingException.class);
  }

  @Test
  @DisplayName("CONNECT 실패 - 토큰이 유효하지 않으면 MessagingException 발생")
  void preSend_connect_fail_invalidToken() {
    
    String token = "invalid-token";
    given(jwtProvider.parse(token)).willThrow(new MoplException(ErrorCode.INVALID_TOKEN));

    Message<?> message = createConnectMessage(token);

    
    assertThatThrownBy(() -> stompAuthInterceptor.preSend(message, messageChannel))
        .isInstanceOf(MessagingException.class);
  }

  @Test
  @DisplayName("SUBSCRIBE 성공 - 참여자면 정상 통과")
  void preSend_subscribe_success() {
    
    UUID conversationId = UUID.randomUUID();
    JwtClaims claims = JwtClaims.builder()
        .userId(myId)
        .email("test@email.com")
        .role("USER")
        .tokenId(UUID.randomUUID().toString())
        .build();

    Conversation conversation = new Conversation(conversationId, myId, otherId, Instant.now());
    given(getConversationUseCase.getById(conversationId, myId)).willReturn(conversation);

    Message<?> message = createSubscribeMessage(
        "/sub/conversations/" + conversationId + "/direct-messages", claims
    );

    
    Message<?> result = stompAuthInterceptor.preSend(message, messageChannel);

    
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("SUBSCRIBE 실패 - 참여자가 아니면 예외 발생")
  void preSend_subscribe_fail_forbidden() {
    
    UUID conversationId = UUID.randomUUID();
    JwtClaims claims = JwtClaims.builder()
        .userId(myId)
        .email("test@email.com")
        .role("USER")
        .tokenId(UUID.randomUUID().toString())
        .build();

    given(getConversationUseCase.getById(conversationId, myId))
        .willThrow(new MoplException(ErrorCode.FORBIDDEN_ACCESS));

    Message<?> message = createSubscribeMessage(
        "/sub/conversations/" + conversationId + "/direct-messages", claims
    );

    
    assertThatThrownBy(() -> stompAuthInterceptor.preSend(message, messageChannel))
        .isInstanceOf(MoplException.class);
  }

  @Test
  @DisplayName("SEND 명령은 검증 없이 그대로 통과")
  void preSend_send_passThrough() {
    
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    
    Message<?> result = stompAuthInterceptor.preSend(message, messageChannel);

    
    assertThat(result).isEqualTo(message);
  }
}