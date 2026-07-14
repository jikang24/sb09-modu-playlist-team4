package com.mopl.global.jwt;

import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenIssuer 테스트")
class AuthTokenIssuerTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenService authTokenService;

    private AuthTokenIssuer authTokenIssuer;

    private UUID userId;
    private UserAuthInfo user;

    @BeforeEach
    void setUp() {
        authTokenIssuer = new AuthTokenIssuer(jwtProvider, authTokenService);
        userId = UUID.randomUUID();
        user = new UserAuthInfo(userId, Instant.now(), "woody@mopl.io", "encoded",
                "woody", null, Role.USER, false);
    }

    private void stubTokenGeneration(String accessToken, String refreshToken, String jti) {
        when(jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER")).thenReturn(accessToken);
        when(jwtProvider.generateRefreshToken(userId, "woody@mopl.io", "USER")).thenReturn(refreshToken);
        when(jwtProvider.calculateTtl(refreshToken)).thenReturn(Duration.ofDays(7));
        when(jwtProvider.parse(accessToken)).thenReturn(JwtClaims.builder().tokenId(jti).build());
        when(jwtProvider.getExpiration(accessToken)).thenReturn(Instant.now().plusSeconds(1800));
    }

    @Test
    @DisplayName("성공: 기존 로그인 세션이 없으면 새 토큰만 발급한다")
    void issue_noExistingSession() {
        when(authTokenService.findAccessJtiByUserId(userId)).thenReturn(Optional.empty());
        stubTokenGeneration("access-token", "refresh-token", "jti-1");
        HttpServletResponse response = new MockHttpServletResponse();

        String accessToken = authTokenIssuer.issue(user, response);

        assertThat(accessToken).isEqualTo("access-token");
        verify(authTokenService, never()).blacklistJti(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(authTokenService).deleteRefreshTokenByUserId(userId);
        verify(authTokenService).saveRefreshToken(userId, "refresh-token", Duration.ofDays(7));
        verify(authTokenService).saveAccessJti(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("jti-1"),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    @DisplayName("성공: 기존 로그인 세션이 살아있으면 기존 액세스 토큰을 블랙리스트에 등록한다")
    void issue_blacklistsExistingSession() {
        AccessJtiEntry existingEntry = new AccessJtiEntry("old-jti", Instant.now().plusSeconds(600));
        when(authTokenService.findAccessJtiByUserId(userId)).thenReturn(Optional.of(existingEntry));
        stubTokenGeneration("access-token", "refresh-token", "jti-2");
        HttpServletResponse response = new MockHttpServletResponse();

        authTokenIssuer.issue(user, response);

        verify(authTokenService).blacklistJti(org.mockito.ArgumentMatchers.eq("old-jti"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    @DisplayName("성공: 기존 액세스 토큰이 이미 만료되었으면 블랙리스트에 등록하지 않는다")
    void issue_expiredExistingSession_doesNotBlacklist() {
        AccessJtiEntry expiredEntry = new AccessJtiEntry("old-jti", Instant.now().minusSeconds(60));
        when(authTokenService.findAccessJtiByUserId(userId)).thenReturn(Optional.of(expiredEntry));
        stubTokenGeneration("access-token", "refresh-token", "jti-3");
        HttpServletResponse response = new MockHttpServletResponse();

        authTokenIssuer.issue(user, response);

        verify(authTokenService, never()).blacklistJti(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("성공: 응답에 REFRESH_TOKEN 쿠키를 설정한다")
    void issue_setsRefreshTokenCookie() {
        when(authTokenService.findAccessJtiByUserId(userId)).thenReturn(Optional.empty());
        stubTokenGeneration("access-token", "refresh-token-value", "jti-4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authTokenIssuer.issue(user, response);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("REFRESH_TOKEN=refresh-token-value");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
    }
}
