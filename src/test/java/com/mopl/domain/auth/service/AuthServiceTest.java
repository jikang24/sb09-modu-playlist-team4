package com.mopl.domain.auth.service;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.event.TempPasswordIssuedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserAuthPort userAuthPort;

    @Mock
    private PasswordResetTokenPort passwordResetTokenPort;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuthService authService;

    private UUID testUserId;
    private UserAuthInfo testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userAuthPort,
                passwordResetTokenPort,
                authTokenService,
                jwtProvider,
                passwordEncoder,
                eventPublisher
        );

        testUserId = UUID.randomUUID();
        testUser = new UserAuthInfo(
                testUserId,
                Instant.now(),
                "test@email.com",
                "encoded_password",
                "Test User",
                "https://example.com/profile.jpg",
                Role.USER,
                false
        );
    }

    @Test
    @DisplayName("성공: 정상적으로 임시 비밀번호를 발급한다")
    void resetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        when(userAuthPort.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_temp_password");

        authService.resetPassword(request);

        verify(userAuthPort).findByEmail("test@email.com");
        verify(passwordResetTokenPort).deleteByUserId(testUserId);
        verify(passwordResetTokenPort).save(any(PasswordResetToken.class));

        ArgumentCaptor<TempPasswordIssuedEvent> eventCaptor = ArgumentCaptor.forClass(TempPasswordIssuedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TempPasswordIssuedEvent event = eventCaptor.getValue();
        assertEquals(testUserId, event.userId());
        assertEquals("test@email.com", event.email());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자로 비밀번호를 초기화하면 예외가 발생한다")
    void resetPassword_UserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest("notfound@email.com");
        when(userAuthPort.findByEmail("notfound@email.com")).thenReturn(Optional.empty());

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(passwordResetTokenPort, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("성공: 이벤트 발행이 확인된다")
    void resetPassword_VerifiesEventPublished() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        when(userAuthPort.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        authService.resetPassword(request);

        ArgumentCaptor<TempPasswordIssuedEvent> captor = ArgumentCaptor.forClass(TempPasswordIssuedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TempPasswordIssuedEvent publishedEvent = captor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(testUserId, publishedEvent.userId());
        assertEquals("test@email.com", publishedEvent.email());
        assertNotNull(publishedEvent.tempPassword());
    }

    @Test
    @DisplayName("성공: 기존 토큰이 삭제된다")
    void resetPassword_DeletesPreviousTokens() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        when(userAuthPort.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        authService.resetPassword(request);

        verify(passwordResetTokenPort).deleteByUserId(testUserId);
    }

    @Test
    @DisplayName("성공: 토큰의 만료시간이 설정된다")
    void resetPassword_TokenExpiryIsSet() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        when(userAuthPort.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        authService.resetPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenPort).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("성공: 유효한 리프레시 토큰으로 새 토큰을 발급한다")
    void refresh() {
        String refreshToken = "refresh_token_123";
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";

        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));

        JwtDto result = authService.refresh(refreshToken);

        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertEquals(testUserId, result.userDto().id());
        assertEquals("test@email.com", result.userDto().email());

        verify(authTokenService).findUserIdByRefreshToken(refreshToken);
        verify(authTokenService).deleteRefreshToken(refreshToken);
        verify(authTokenService).saveRefreshToken(testUserId, newRefreshToken, Duration.ofDays(7));
        verify(userAuthPort).findById(testUserId);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 리프레시 토큰으로 예외가 발생한다")
    void refresh_RefreshTokenNotFound() {
        String refreshToken = "invalid_token";
        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.empty());

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.refresh(refreshToken);
        });

        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
        verify(authTokenService).findUserIdByRefreshToken(refreshToken);
        verify(authTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자로 토큰 재발급을 시도하면 예외가 발생한다")
    void refresh_UserNotFound() {
        String refreshToken = "refresh_token_123";
        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.empty());

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.refresh(refreshToken);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패: 잠긴 사용자로 토큰 재발급을 시도하면 예외가 발생한다")
    void refresh_UserLocked() {
        String refreshToken = "refresh_token_123";
        UserAuthInfo lockedUser = new UserAuthInfo(
                testUserId,
                Instant.now(),
                "test@email.com",
                "encoded_password",
                "Test User",
                "https://example.com/profile.jpg",
                Role.USER,
                true
        );

        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(lockedUser));

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.refresh(refreshToken);
        });

        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
        verify(authTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    @DisplayName("성공: 이전 토큰을 삭제한 후 새 토큰을 발급한다")
    void refresh_TokenDeletedBeforeIssuing() {
        String refreshToken = "refresh_token_123";
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";

        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));

        JwtDto result = authService.refresh(refreshToken);

        verify(authTokenService).findUserIdByRefreshToken(refreshToken);
        verify(authTokenService).deleteRefreshToken(refreshToken);
        verify(authTokenService).saveRefreshToken(testUserId, newRefreshToken, Duration.ofDays(7));
    }

    @Test
    @DisplayName("성공: 새로운 액세스 토큰과 리프레시 토큰이 생성된다")
    void refresh_GeneratesNewTokens() {
        String refreshToken = "old_token";
        String newAccessToken = "new_access_token_xyz";
        String newRefreshToken = "new_refresh_token_xyz";

        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));

        JwtDto result = authService.refresh(refreshToken);

        assertEquals(newAccessToken, result.accessToken());
        verify(jwtProvider).generateAccessToken(testUserId, "test@email.com", "USER");
        verify(jwtProvider).generateRefreshToken(testUserId, "test@email.com", "USER");
    }

    @Test
    @DisplayName("성공: 응답에 사용자 정보가 포함된다")
    void refresh_UserInfoIncludedInResponse() {
        String refreshToken = "refresh_token_123";
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";

        when(authTokenService.findUserIdByRefreshToken(refreshToken))
                .thenReturn(Optional.of(testUserId));
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));

        JwtDto result = authService.refresh(refreshToken);

        assertEquals(testUserId, result.userDto().id());
        assertEquals("test@email.com", result.userDto().email());
        assertEquals("Test User", result.userDto().name());
    }
}
