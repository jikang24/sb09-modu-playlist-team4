package com.mopl.domain.notification.service;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.domain.notification.dto.NotificationSortBy;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.domain.notification.sse.SseNotificationSender;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import com.mopl.domain.notification.support.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private SseNotificationSender sseNotificationSender;

  private NotificationServiceImpl notificationService;

  private UUID userId;
  private UUID otherUserId;

  @Mock
  private CurrentUserProvider currentUserProvider;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationServiceImpl(
        notificationRepository,
        sseNotificationSender,
        currentUserProvider
    );
    userId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
  }

  private void authenticate(UUID authenticatedUserId) {
    given(currentUserProvider.getCurrentUserId())
        .willReturn(authenticatedUserId);
  }

  private Notification notification(UUID id, UUID receiverId, Instant createdAt) {
    return Notification.builder()
        .id(id)
        .receiverId(receiverId)
        .type(NotificationType.FOLLOW)
        .title("새 팔로워")
        .content("누군가 팔로우했습니다.")
        .createdAt(createdAt)
        .build();
  }

  @Nested
  @DisplayName("send")
  class Send {

    @Test
    @DisplayName("성공: 알림을 저장하고 SSE로 전송한다")
    void send_success() {
      UUID receiverId = UUID.randomUUID();
      Notification saved = notification(UUID.randomUUID(), receiverId, Instant.now());
      given(notificationRepository.save(any(Notification.class))).willReturn(saved);

      NotificationDto result = notificationService.send(
          receiverId, NotificationType.FOLLOW, "제목", "내용");

      assertThat(result.receiverId()).isEqualTo(receiverId);
      assertThat(result.title()).isEqualTo("새 팔로워");
      verify(sseNotificationSender).send(eq(receiverId), any(NotificationDto.class));
    }
  }

  @Nested
  @DisplayName("findMyNotifications")
  class FindMyNotifications {

    private NotificationSearchRequest baseRequest(int limit) {
      return new NotificationSearchRequest(
          null, null, limit, SortDirection.ASCENDING, NotificationSortBy.createdAt
      );
    }

    @Test
    @DisplayName("성공: 다음 페이지가 있으면 hasNext=true이다")
    void findMyNotifications_hasNext() {
      authenticate(userId);
      NotificationSearchRequest request = baseRequest(2);
      Instant now = Instant.parse("2026-01-01T00:00:00Z");
      Notification n1 = notification(UUID.randomUUID(), userId, now);
      Notification n2 = notification(UUID.randomUUID(), userId, now.plusSeconds(1));
      Notification n3 = notification(UUID.randomUUID(), userId, now.plusSeconds(2));

      given(notificationRepository.findUnreadByReceiverWithCursor(userId, request))
          .willReturn(List.of(n1, n2, n3));
      given(notificationRepository.countByReceiverIdAndIsReadFalse(userId)).willReturn(5L);

      var result = notificationService.findMyNotifications(request);

      assertThat(result.hasNext()).isTrue();
      assertThat(result.data()).hasSize(2);
      assertThat(result.nextCursor()).isEqualTo(n2.getCreatedAt().toString());
      assertThat(result.nextIdAfter()).isEqualTo(n2.getId());
      assertThat(result.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("성공: 다음 페이지가 없으면 hasNext=false이다")
    void findMyNotifications_noNext() {
      authenticate(userId);
      NotificationSearchRequest request = baseRequest(5);
      Notification n1 = notification(UUID.randomUUID(), userId, Instant.now());

      given(notificationRepository.findUnreadByReceiverWithCursor(userId, request))
          .willReturn(List.of(n1));
      given(notificationRepository.countByReceiverIdAndIsReadFalse(userId)).willReturn(1L);

      var result = notificationService.findMyNotifications(request);

      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextIdAfter()).isNull();
    }

    @Test
    @DisplayName("실패: limit이 1 미만이면 INVALID_NOTIFICATION_SEARCH_REQUEST")
    void findMyNotifications_invalidLimit() {
      NotificationSearchRequest request = baseRequest(0);

      assertThatThrownBy(() -> notificationService.findMyNotifications(request))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_NOTIFICATION_SEARCH_REQUEST);
    }

    @Test
    @DisplayName("실패: cursor만 있으면 INVALID_NOTIFICATION_CURSOR")
    void findMyNotifications_cursorWithoutIdAfter() {
      NotificationSearchRequest request = new NotificationSearchRequest(
          "2026-01-01T00:00:00Z", null, 10, SortDirection.ASCENDING, NotificationSortBy.createdAt
      );

      assertThatThrownBy(() -> notificationService.findMyNotifications(request))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_NOTIFICATION_CURSOR);
    }

    @Test
    @DisplayName("실패: idAfter만 있으면 INVALID_NOTIFICATION_CURSOR")
    void findMyNotifications_idAfterWithoutCursor() {
      NotificationSearchRequest request = new NotificationSearchRequest(
          null, UUID.randomUUID(), 10, SortDirection.ASCENDING, NotificationSortBy.createdAt
      );

      assertThatThrownBy(() -> notificationService.findMyNotifications(request))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_NOTIFICATION_CURSOR);
    }

    @Test
    @DisplayName("실패: 잘못된 cursor 형식이면 INVALID_NOTIFICATION_CURSOR")
    void findMyNotifications_invalidCursorFormat() {
      NotificationSearchRequest request = new NotificationSearchRequest(
          "invalid-cursor", UUID.randomUUID(), 10, SortDirection.ASCENDING, NotificationSortBy.createdAt
      );

      assertThatThrownBy(() -> notificationService.findMyNotifications(request))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_NOTIFICATION_CURSOR);
    }

    @Test
    @DisplayName("실패: 인증 정보가 없으면 INVALID_TOKEN")
    void findMyNotifications_noAuthentication() {
      NotificationSearchRequest request = baseRequest(10);

      given(currentUserProvider.getCurrentUserId())
          .willThrow(new MoplException(ErrorCode.INVALID_TOKEN));

      assertThatThrownBy(() -> notificationService.findMyNotifications(request))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
  }

  @Nested
  @DisplayName("read")
  class Read {

    @Test
    @DisplayName("성공: 본인 알림을 읽음 처리한다")
    void read_success() {
      authenticate(userId);
      UUID notificationId = UUID.randomUUID();
      Notification notification = notification(notificationId, userId, Instant.now());
      given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

      notificationService.read(notificationId);

      assertThat(notification.isRead()).isTrue();
    }

    @Test
    @DisplayName("실패: 알림이 없으면 NOTIFICATION_NOT_FOUND")
    void read_notFound() {
      UUID notificationId = UUID.randomUUID();
      given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> notificationService.read(notificationId))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 다른 사용자 알림이면 NOTIFICATION_ACCESS_DENIED")
    void read_accessDenied() {
      authenticate(userId);
      UUID notificationId = UUID.randomUUID();
      Notification notification = notification(notificationId, otherUserId, Instant.now());
      given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

      assertThatThrownBy(() -> notificationService.read(notificationId))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);

      assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("실패: 인증 정보가 없으면 INVALID_TOKEN")
    void read_noAuthentication() {
      UUID notificationId = UUID.randomUUID();
      Notification notification = notification(notificationId, userId, Instant.now());

      given(notificationRepository.findById(notificationId))
          .willReturn(Optional.of(notification));

      given(currentUserProvider.getCurrentUserId())
          .willThrow(new MoplException(ErrorCode.INVALID_TOKEN));

      assertThatThrownBy(() -> notificationService.read(notificationId))
          .isInstanceOf(MoplException.class)
          .extracting(e -> ((MoplException) e).getErrorCode())
          .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
  }
}
