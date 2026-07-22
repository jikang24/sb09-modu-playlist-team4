package com.mopl.global.jwt;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenService authTokenService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider, authTokenService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private JwtClaims claimsOf(UUID userId) {
        return JwtClaims.builder().userId(userId).email("woody@mopl.io").role("USER").tokenId("jti-1").build();
    }

    @Test
    @DisplayName("성공: 유효한 Bearer 토큰이면 SecurityContext에 인증 정보를 설정한다")
    void validToken_authenticates() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtProvider.parse("valid-token")).thenReturn(claimsOf(userId));
        when(authTokenService.isBlacklistedJti("jti-1")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        JwtClaims principal = (JwtClaims) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("성공: Authorization 헤더가 없으면 인증 없이 다음 필터로 통과시킨다")
    void noToken_passesThroughUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("실패: 블랙리스트에 등록된 토큰이면 인증하지 않는다")
    void blacklistedToken_doesNotAuthenticate() throws Exception {
        when(jwtProvider.parse("blacklisted-token")).thenReturn(claimsOf(UUID.randomUUID()));
        when(authTokenService.isBlacklistedJti("jti-1")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer blacklisted-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("실패: 유효하지 않은 토큰이면 인증하지 않고 예외를 전파하지 않는다")
    void invalidToken_doesNotAuthenticateOrThrow() throws Exception {
        when(jwtProvider.parse("invalid-token")).thenThrow(new MoplException(ErrorCode.INVALID_TOKEN));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("성공: Bearer 형식이 아닌 Authorization 헤더는 무시한다")
    void nonBearerAuthorizationHeader_ignored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).parse(anyString());
    }

    @Test
    @DisplayName("성공: SSE 요청이고 유효한 refresh 쿠키가 있으면 silent refresh로 인증을 재발급한다")
    void sseRequest_withValidRefreshCookie_silentlyRefreshes() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtClaims refreshClaims = claimsOf(userId);
        JwtClaims newAccessClaims = JwtClaims.builder()
                .userId(userId).email("woody@mopl.io").role("USER").tokenId("new-jti").build();

        when(jwtProvider.parse("refresh-token-value")).thenReturn(refreshClaims);
        when(authTokenService.isValidRefreshToken(userId, "refresh-token-value")).thenReturn(true);
        when(jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER")).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken(userId, "woody@mopl.io", "USER")).thenReturn("new-refresh-token");
        when(jwtProvider.calculateTtl("new-refresh-token")).thenReturn(Duration.ofDays(7));
        when(jwtProvider.parse("new-access-token")).thenReturn(newAccessClaims);
        when(jwtProvider.getExpiration("new-access-token")).thenReturn(Instant.now().plusSeconds(1800));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sse/notifications");
        request.setCookies(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh-token-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        JwtClaims principal = (JwtClaims) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getTokenId()).isEqualTo("new-jti");
        assertThat(response.getHeader("Set-Cookie")).contains("REFRESH_TOKEN=new-refresh-token");
        verify(authTokenService).saveRefreshToken(userId, "new-refresh-token", Duration.ofDays(7));
    }

    @Test
    @DisplayName("성공: SSE 요청인데 refresh 쿠키가 없으면 아무 것도 하지 않는다")
    void sseRequest_noRefreshCookie_noOp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sse/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).parse(anyString());
    }

    @Test
    @DisplayName("실패: SSE 요청이고 refresh 토큰이 이미 폐기되었으면 재발급하지 않는다")
    void sseRequest_revokedRefreshToken_doesNotRefresh() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtProvider.parse("revoked-refresh-token")).thenReturn(claimsOf(userId));
        when(authTokenService.isValidRefreshToken(userId, "revoked-refresh-token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sse/notifications");
        request.setCookies(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "revoked-refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authTokenService, never()).saveRefreshToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("성공: SSE가 아닌 요청은 인증 실패 시 silent refresh를 시도하지 않는다")
    void nonSseRequest_doesNotAttemptSilentRefresh() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/playlists");
        request.setCookies(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "refresh-token-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authTokenService, never()).isValidRefreshToken(any(), anyString());
    }
}
