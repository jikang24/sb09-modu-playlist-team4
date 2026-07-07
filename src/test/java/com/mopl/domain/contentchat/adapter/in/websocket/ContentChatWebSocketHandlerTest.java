package com.mopl.domain.contentchat.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.contentchat.adapter.in.websocket.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.adapter.port.LoadUserPort;
import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtClaims;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ContentChatWebSocketHandlerTest {

  @InjectMocks
  private ContentChatWebSocketHandler handler;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

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
  @DisplayName("채팅 전송 성공 - DB 저장 없이 /sub/contents/{contentId}/chat 로 그대로 릴레이된다")
  void sendMessage_success() {
    JwtClaims claims = createClaims(senderId);
    ContentChatSendRequest request = new ContentChatSendRequest("안녕하세요");
    UserSummary sender = new UserSummary(senderId, "sender", null);
    given(loadUserPort.getUserSummary(senderId)).willReturn(sender);

    SimpMessageHeaderAccessor accessor = createAccessorWithClaims(claims);

    handler.sendMessage(contentId, request, accessor);

    ArgumentCaptor<ContentChatDto> captor = ArgumentCaptor.forClass(ContentChatDto.class);
    then(messagingTemplate).should().convertAndSend(
        eq("/sub/contents/" + contentId + "/chat"), captor.capture());

    ContentChatDto dto = captor.getValue();
    assertThat(dto.sender()).isEqualTo(sender);
    assertThat(dto.content()).isEqualTo("안녕하세요");
  }
}