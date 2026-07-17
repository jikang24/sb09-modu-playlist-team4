package com.mopl.domain.user.event;

import com.mopl.global.jwt.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLockedEventListener 테스트")
class UserLockedEventListenerTest {

    @Mock
    private AuthTokenService authTokenService;

    private UserLockedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserLockedEventListener(authTokenService);
    }

    @Test
    @DisplayName("성공: 계정 잠금 이벤트를 받으면 기존 세션을 강제 로그아웃시킨다")
    void onUserLocked_forcesLogout() {
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId);

        listener.onUserLocked(event);

        verify(authTokenService).forceLogoutByUserId(userId);
    }
}
