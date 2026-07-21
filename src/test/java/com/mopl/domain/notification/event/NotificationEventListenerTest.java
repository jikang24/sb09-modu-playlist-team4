package com.mopl.domain.notification.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationLevel;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.event.NotificationRequestedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener 테스트")
class NotificationEventListenerTest {

  @Mock
  private NotificationService notificationService;

  private NotificationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificationEventListener(notificationService);
  }

  @Test
  @DisplayName("성공: 유효한 알림 이벤트를 처리한다")
  void handle_validEvent() {
    UUID receiverId = UUID.randomUUID();
    NotificationRequestedEvent event = new NotificationRequestedEvent(
        receiverId,
        NotificationType.FOLLOW.name(),
        "새 팔로워",
        "누군가 팔로우했습니다."
    );
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(),
        Instant.now(),
        receiverId,
        "새 팔로워",
        "누군가 팔로우했습니다.",
        NotificationLevel.INFO
    );
    given(notificationService.send(
        eq(event.eventId()),
        eq(receiverId),
        eq(NotificationType.FOLLOW),
        eq("새 팔로워"),
        eq("누군가 팔로우했습니다.")
    )).willReturn(dto);

    listener.handle(event);

    verify(notificationService).send(
        event.eventId(),
        receiverId,
        NotificationType.FOLLOW,
        "새 팔로워",
        "누군가 팔로우했습니다."
    );
  }

  @Test
  @DisplayName("성공: 알 수 없는 알림 타입은 무시한다")
  void handle_unknownType() {
    UUID receiverId = UUID.randomUUID();
    NotificationRequestedEvent event = new NotificationRequestedEvent(
        receiverId,
        "UNKNOWN_TYPE",
        "제목",
        "내용"
    );

    listener.handle(event);

    verify(notificationService, never()).send(
        any(), any(), any(), any(), any()
    );
  }
}
