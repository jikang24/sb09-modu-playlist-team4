package com.mopl.domain.user.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.jwt.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRoleChangedEventListener 테스트")
class UserRoleChangedEventListenerTest {

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private AuthTokenService authTokenService;

    private UserRoleChangedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserRoleChangedEventListener(notificationEventPublisher, authTokenService);
    }

    @Test
    @DisplayName("성공: 권한 변경 이벤트를 받아 알림 요청 이벤트를 발행한다")
    void onRoleChanged_publishesNotification() {
        UUID userId = UUID.randomUUID();
        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, Role.USER, Role.ADMIN);

        listener.onRoleChanged(event);

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(notificationEventPublisher).publish(captor.capture());

        NotificationRequestedEvent published = captor.getValue();
        assertThat(published.receiverId()).isEqualTo(userId);
        assertThat(published.type()).isEqualTo(NotificationType.ROLE_CHANGED.name());
        assertThat(published.content()).contains("USER").contains("ADMIN");
    }

    @Test
    @DisplayName("성공: 권한 변경 이벤트를 받으면 기존 세션을 강제 로그아웃시킨다")
    void onRoleChanged_forcesLogout() {
        UUID userId = UUID.randomUUID();
        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, Role.USER, Role.ADMIN);

        listener.onRoleChanged(event);

        verify(authTokenService).forceLogoutByUserId(userId);
    }
}
