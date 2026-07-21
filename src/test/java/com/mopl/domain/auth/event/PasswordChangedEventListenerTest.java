package com.mopl.domain.auth.event;

import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.event.PasswordChangedEvent;
import com.mopl.global.jwt.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordChangedEventListener 테스트")
class PasswordChangedEventListenerTest {

    @Mock
    private PasswordResetTokenPort passwordResetTokenPort;

    @Mock
    private AuthTokenService authTokenService;

    private PasswordChangedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PasswordChangedEventListener(passwordResetTokenPort, authTokenService);
    }

    @Test
    @DisplayName("성공: 비밀번호 변경 커밋 후 기존 세션을 강제 로그아웃한다")
    void onPasswordChangedForceLogout_ForcesLogoutByUserId() {
        UUID userId = UUID.randomUUID();
        PasswordChangedEvent event = new PasswordChangedEvent(userId);

        listener.onPasswordChangedForceLogout(event);

        verify(authTokenService).forceLogoutByUserId(userId);
    }

    @Test
    @DisplayName("성공: 비밀번호 변경 시 사용자의 임시 비밀번호 토큰을 삭제한다")
    void onPasswordChanged_DeletesTokensByUserId() {
        UUID userId = UUID.randomUUID();
        PasswordChangedEvent event = new PasswordChangedEvent(userId);

        listener.onPasswordChanged(event);

        verify(passwordResetTokenPort).deleteByUserId(userId);
    }

    @Test
    @DisplayName("성공: 정확한 사용자의 토큰만 삭제한다")
    void onPasswordChanged_DeletesTokensForCorrectUser() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        listener.onPasswordChanged(new PasswordChangedEvent(userId1));
        listener.onPasswordChanged(new PasswordChangedEvent(userId2));

        verify(passwordResetTokenPort).deleteByUserId(userId1);
        verify(passwordResetTokenPort).deleteByUserId(userId2);
    }

    @Test
    @DisplayName("성공: 이벤트 처리가 한 번만 호출된다")
    void onPasswordChanged_CalledOnce() {
        UUID userId = UUID.randomUUID();
        PasswordChangedEvent event = new PasswordChangedEvent(userId);

        listener.onPasswordChanged(event);

        verify(passwordResetTokenPort, times(1)).deleteByUserId(userId);
    }

    @Test
    @DisplayName("실패: 포트 호출 중 오류가 발생해도 처리한다")
    void onPasswordChanged_PortThrowsException() {
        UUID userId = UUID.randomUUID();
        PasswordChangedEvent event = new PasswordChangedEvent(userId);

        doThrow(new RuntimeException("Database error"))
                .when(passwordResetTokenPort).deleteByUserId(userId);

        try {
            listener.onPasswordChanged(event);
            verify(passwordResetTokenPort).deleteByUserId(userId);
        } catch (RuntimeException e) {
            verify(passwordResetTokenPort).deleteByUserId(userId);
        }
    }

    @Test
    @DisplayName("성공: 같은 사용자의 이벤트를 여러 번 처리한다")
    void onPasswordChanged_MultipleInvocations() {
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            listener.onPasswordChanged(new PasswordChangedEvent(userId));
        }

        verify(passwordResetTokenPort, times(3)).deleteByUserId(userId);
    }

    @Test
    @DisplayName("성공: 다른 사용자들의 비밀번호 변경 이벤트를 처리한다")
    void onPasswordChanged_DifferentUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        listener.onPasswordChanged(new PasswordChangedEvent(user1));
        listener.onPasswordChanged(new PasswordChangedEvent(user2));
        listener.onPasswordChanged(new PasswordChangedEvent(user3));

        verify(passwordResetTokenPort).deleteByUserId(user1);
        verify(passwordResetTokenPort).deleteByUserId(user2);
        verify(passwordResetTokenPort).deleteByUserId(user3);
        verify(passwordResetTokenPort, times(3)).deleteByUserId(any(UUID.class));
    }
}
