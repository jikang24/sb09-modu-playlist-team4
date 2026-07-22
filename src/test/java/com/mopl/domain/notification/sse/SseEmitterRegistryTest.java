package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationLevel;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseEmitterRegistry 테스트")
class SseEmitterRegistryTest {

  @Mock
  private NotificationRepository notificationRepository;

  private SseEmitterRegistry registry;

  private UUID userId;

  @BeforeEach
  void setUp() {
    registry = new SseEmitterRegistry(notificationRepository);
    userId = UUID.randomUUID();
  }

  private Notification notification(UUID id, Instant createdAt) {
    return Notification.builder()
        .id(id)
        .receiverId(userId)
        .type(NotificationType.DIRECT_MESSAGE)
        .title("DM")
        .content("새 메시지")
        .createdAt(createdAt)
        .build();
  }

  @Nested
  @DisplayName("connect")
  class Connect {

    @Test
    @DisplayName("성공: lastEventId 없이 연결하면 emitter를 반환한다")
    void connect_withoutLastEventId() {
      SseEmitter emitter = registry.connect(userId, null);

      assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("성공: lastEventId 이후 알림을 replay한다")
    void connect_withLastEventId_replay() {
      UUID lastEventId = UUID.randomUUID();
      Instant base = Instant.parse("2026-01-01T00:00:00Z");
      Notification last = notification(lastEventId, base);
      Notification replayed = notification(UUID.randomUUID(), base.plusSeconds(1));

      given(notificationRepository.findByReceiverIdAfter(userId, lastEventId, 201))
          .willReturn(List.of(replayed));

      SseEmitter emitter = registry.connect(userId, lastEventId);

      assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("성공: lastEventId에 해당하는 알림이 없으면 빈 replay 후 연결한다")
    void connect_withUnknownLastEventId() {
      UUID lastEventId = UUID.randomUUID();
      given(notificationRepository.findByReceiverIdAfter(userId, lastEventId, 201))
          .willReturn(List.of());

      SseEmitter emitter = registry.connect(userId, lastEventId);

      assertThat(emitter).isNotNull();
    }
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("성공: 연결된 클라이언트에 알림을 전송한다")
    void send_toConnectedClient() {
      registry.connect(userId, null);
      NotificationDto dto = new NotificationDto(
          UUID.randomUUID(),
          Instant.now(),
          userId,
          "제목",
          "내용",
          NotificationLevel.INFO
      );

      registry.send(userId, dto);
    }

    @Test
    @DisplayName("성공: 연결된 클라이언트가 없으면 아무 동작도 하지 않는다")
    void send_noConnectedClient() {
      NotificationDto dto = new NotificationDto(
          UUID.randomUUID(),
          Instant.now(),
          userId,
          "제목",
          "내용",
          NotificationLevel.INFO
      );

      registry.send(userId, dto);
    }

    @Test
    @DisplayName("성공: 완료된 emitter에 전송하면 실패해도 예외를 던지지 않는다")
    void send_completedEmitter() {
      SseEmitter emitter = registry.connect(userId, null);
      emitter.complete();

      NotificationDto dto = new NotificationDto(
          UUID.randomUUID(),
          Instant.now(),
          userId,
          "제목",
          "내용",
          NotificationLevel.INFO
      );

      registry.send(userId, dto);
    }

    @Test
    @DisplayName("성공: completion 콜백으로 연결을 정리한다")
    void send_afterCompletionCallback() {
      SseEmitter emitter = registry.connect(userId, null);
      emitter.complete();

      NotificationDto dto = new NotificationDto(
          UUID.randomUUID(),
          Instant.now(),
          userId,
          "제목",
          "내용",
          NotificationLevel.INFO
      );

      registry.send(userId, dto);
    }
  }

  @Nested
  @DisplayName("sendDirectMessage")
  class SendDirectMessage {

    private DirectMessageDto directMessageDto() {
      return new DirectMessageDto(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Instant.now(),
          new UserSummary(UUID.randomUUID(), "sender", null),
          new UserSummary(userId, "receiver", null),
          "안녕하세요"
      );
    }

    @Test
    @DisplayName("성공: 연결된 클라이언트에 direct-messages 이벤트로 전송한다")
    void sendDirectMessage_toConnectedClient() {
      registry.connect(userId, null);

      registry.sendDirectMessage(userId, directMessageDto());
    }

    @Test
    @DisplayName("성공: 연결된 클라이언트가 없으면 아무 동작도 하지 않는다")
    void sendDirectMessage_noConnectedClient() {
      registry.sendDirectMessage(userId, directMessageDto());
    }

    @Test
    @DisplayName("성공: 완료된 emitter에 전송하면 실패해도 예외를 던지지 않는다")
    void sendDirectMessage_completedEmitter() {
      SseEmitter emitter = registry.connect(userId, null);
      emitter.complete();

      registry.sendDirectMessage(userId, directMessageDto());
    }
  }
}
