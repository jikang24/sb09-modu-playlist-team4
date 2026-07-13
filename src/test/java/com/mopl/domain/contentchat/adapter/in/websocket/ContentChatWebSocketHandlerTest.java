package com.mopl.domain.contentchat.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.contentchat.adapter.in.websocket.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.adapter.port.LoadUserPort;
import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.global.config.RedisConfig;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtClaims;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@ExtendWith(MockitoExtension.class)
class ContentChatWebSocketHandlerTest {

  @InjectMocks
  private ContentChatWebSocketHandler handler;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ObjectMapper objectMapper;

  private final UUID senderId = UUID.randomUUID();
  private final UUID contentId = UUID.randomUUID();

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
  @DisplayName("채팅 전송 성공 - DB 저장 없이 Redis pub/sub(고정 채널)으로 발행되어 /sub/contents/{contentId}/chat 로 릴레이된다")
  void sendMessage_success() throws Exception {
    JwtClaims claims = createClaims(senderId);
    ContentChatSendRequest request = new ContentChatSendRequest("안녕하세요");
    UserSummary sender = new UserSummary(senderId, "sender", null);
    given(loadUserPort.getUserSummary(senderId)).willReturn(sender);

    String jsonPayload = "{\"destination\":\"contents/" + contentId + "/chat\",\"payload\":{}}";
    given(objectMapper.writeValueAsString(any())).willReturn(jsonPayload);

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    handler.sendMessage(contentId, request, accessor);

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    then(objectMapper).should().writeValueAsString(captor.capture());
    Map<String, Object> message = captor.getValue();
    assertThat(message.get("destination")).isEqualTo("contents/" + contentId + "/chat");
    ContentChatDto dto = (ContentChatDto) message.get("payload");
    assertThat(dto.sender()).isEqualTo(sender);
    assertThat(dto.content()).isEqualTo("안녕하세요");

    then(redisTemplate).should().convertAndSend(RedisConfig.CONTENT_CHAT_CHANNEL, jsonPayload);
  }
}