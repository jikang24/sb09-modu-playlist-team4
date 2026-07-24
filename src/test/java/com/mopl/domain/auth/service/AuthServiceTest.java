package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.RefreshResult;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserAuthPort userAuthPort;

    @Mock
    private PasswordResetWriteService passwordResetWriteService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private JwtProvider jwtProvider;

    private AuthService authService;

    private UUID testUserId;
    private UserAuthInfo testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userAuthPort,
                passwordResetWriteService,
                authTokenService,
                jwtProvider
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

        authService.resetPassword(request);

        verify(userAuthPort).findByEmail("test@email.com");

        ArgumentCaptor<String> tempPasswordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetWriteService).resetPassword(eq(testUserId), eq("test@email.com"), tempPasswordCaptor.capture());
        assertNotNull(tempPasswordCaptor.getValue());
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
        verify(passwordResetWriteService, never()).resetPassword(any(), any(), any());
    }

    @Test
    @DisplayName("성공: 유효한 리프레시 토큰으로 새 토큰을 발급한다")
    void refresh() {
        String refreshToken = "refresh_token_123";
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";

        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(true);
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));
        Instant accessExpiry = Instant.now().plus(Duration.ofMinutes(30));
        when(jwtProvider.parse(newAccessToken))
                .thenReturn(JwtClaims.builder().tokenId("new-access-jti").build());
        when(jwtProvider.getExpiration(newAccessToken)).thenReturn(accessExpiry);

        RefreshResult result = authService.refresh(refreshToken);

        assertNotNull(result);
        assertEquals(newAccessToken, result.jwtDto().accessToken());
        assertEquals(testUserId, result.jwtDto().userDto().id());
        assertEquals("test@email.com", result.jwtDto().userDto().email());
        assertEquals(newRefreshToken, result.refreshToken());
        assertEquals(Duration.ofDays(7), result.refreshTokenTtl());

        verify(authTokenService).isValidRefreshToken(testUserId, refreshToken);
        verify(authTokenService).saveRefreshToken(testUserId, newRefreshToken, Duration.ofDays(7));
        verify(authTokenService).saveAccessJti(testUserId, "new-access-jti", accessExpiry);
        verify(userAuthPort).findById(testUserId);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 리프레시 토큰으로 예외가 발생한다")
    void refresh_RefreshTokenNotFound() {
        String refreshToken = "invalid_token";
        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(false);

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.refresh(refreshToken);
        });

        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
        verify(authTokenService).isValidRefreshToken(testUserId, refreshToken);
        verify(authTokenService, never()).saveRefreshToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자로 토큰 재발급을 시도하면 예외가 발생한다")
    void refresh_UserNotFound() {
        String refreshToken = "refresh_token_123";
        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(true);
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

        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(true);
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(lockedUser));

        MoplException exception = assertThrows(MoplException.class, () -> {
            authService.refresh(refreshToken);
        });

        assertEquals(ErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        verify(authTokenService, never()).saveRefreshToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("성공: 새로운 액세스 토큰과 리프레시 토큰이 생성된다")
    void refresh_GeneratesNewTokens() {
        String refreshToken = "old_token";
        String newAccessToken = "new_access_token_xyz";
        String newRefreshToken = "new_refresh_token_xyz";

        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(true);
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));
        when(jwtProvider.parse(newAccessToken))
                .thenReturn(JwtClaims.builder().tokenId("new-access-jti").build());
        when(jwtProvider.getExpiration(newAccessToken)).thenReturn(Instant.now().plus(Duration.ofMinutes(30)));

        RefreshResult result = authService.refresh(refreshToken);

        assertEquals(newAccessToken, result.jwtDto().accessToken());
        verify(jwtProvider).generateAccessToken(testUserId, "test@email.com", "USER");
        verify(jwtProvider).generateRefreshToken(testUserId, "test@email.com", "USER");
    }

    @Test
    @DisplayName("성공: 응답에 사용자 정보가 포함된다")
    void refresh_UserInfoIncludedInResponse() {
        String refreshToken = "refresh_token_123";
        String newAccessToken = "new_access_token";
        String newRefreshToken = "new_refresh_token";

        when(jwtProvider.parse(refreshToken))
                .thenReturn(JwtClaims.builder().userId(testUserId).build());
        when(authTokenService.isValidRefreshToken(testUserId, refreshToken)).thenReturn(true);
        when(userAuthPort.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken(testUserId, "test@email.com", "USER"))
                .thenReturn(newRefreshToken);
        when(jwtProvider.calculateTtl(newRefreshToken)).thenReturn(Duration.ofDays(7));
        when(jwtProvider.parse(newAccessToken))
                .thenReturn(JwtClaims.builder().tokenId("new-access-jti").build());
        when(jwtProvider.getExpiration(newAccessToken)).thenReturn(Instant.now().plus(Duration.ofMinutes(30)));

        RefreshResult result = authService.refresh(refreshToken);

        assertEquals(testUserId, result.jwtDto().userDto().id());
        assertEquals("test@email.com", result.jwtDto().userDto().email());
        assertEquals("Test User", result.jwtDto().userDto().name());
    }
}
