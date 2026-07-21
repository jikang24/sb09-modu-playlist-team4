package com.mopl.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisMessageSubscriber 테스트")
class RedisMessageSubscriberTest {

  private static final byte[] CHANNEL = "dm-channel".getBytes(StandardCharsets.UTF_8);

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  private RedisMessageSubscriber subscriber;

  @BeforeEach
  void setUp() {
    // JSON 파싱이 실제 동작의 핵심이므로 ObjectMapper는 mock이 아닌 실제 객체를 사용한다
    subscriber = new RedisMessageSubscriber(messagingTemplate, new ObjectMapper());
  }

  private Message redisMessage(String body) {
    return new DefaultMessage(CHANNEL, body.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("정상 메시지를 받으면 /sub/ 프리픽스를 붙인 destination으로 payload를 전달한다")
  void onMessage_success() {
    // given
    String body = """
        {
          "destination": "conversations/123e4567-e89b-12d3-a456-426614174000/direct-messages",
          "payload": {"content": "안녕하세요", "senderId": "abc"}
        }
        """;

    // when
    subscriber.onMessage(redisMessage(body), null);

    // then
    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/sub/conversations/123e4567-e89b-12d3-a456-426614174000/direct-messages"),
        payloadCaptor.capture()
    );

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
    assertThat(payload)
        .containsEntry("content", "안녕하세요")
        .containsEntry("senderId", "abc");
  }

  @Test
  @DisplayName("잘못된 JSON이 들어와도 예외를 밖으로 던지지 않는다 (리스너 스레드 보호)")
  void onMessage_invalidJson_doesNotThrow() {
    // given
    Message invalid = redisMessage("this is not json");

    // when & then
    assertThatCode(() -> subscriber.onMessage(invalid, null))
        .doesNotThrowAnyException();

    verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
  }

  @Test
  @DisplayName("WebSocket 전달(convertAndSend)이 실패해도 예외를 밖으로 던지지 않는다")
  void onMessage_convertAndSendFails_doesNotThrow() {
    // given
    String body = """
        {"destination": "conversations/1/direct-messages", "payload": {"content": "hi"}}
        """;
    doThrow(new MessagingException("브로커 전송 실패"))
        .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

    // when & then
    assertThatCode(() -> subscriber.onMessage(redisMessage(body), null))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("payload가 없는 메시지도 처리 중 예외를 밖으로 던지지 않는다")
  void onMessage_missingPayload_doesNotThrow() {
    // given: payload 필드 자체가 없는 비정상 메시지
    String body = """
        {"destination": "conversations/1/direct-messages"}
        """;

    // when & then
    assertThatCode(() -> subscriber.onMessage(redisMessage(body), null))
        .doesNotThrowAnyException();
  }
}
