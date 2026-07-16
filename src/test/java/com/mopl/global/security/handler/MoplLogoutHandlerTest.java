package com.mopl.global.security.handler;

import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoplLogoutHandler 테스트")
class MoplLogoutHandlerTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenService authTokenService;

    private MoplLogoutHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplLogoutHandler(jwtProvider, authTokenService);
    }

    @Test
    @DisplayName("성공: 유효한 토큰이면 블랙리스트 등록 및 리프레시/액세스 토큰을 삭제한다")
    void logout_validToken_blacklistsAndDeletesTokens() {
        UUID userId = UUID.randomUUID();
        JwtClaims claims = JwtClaims.builder().userId(userId).tokenId("jti-1").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer raw-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.getExpiration("raw-token")).thenReturn(Instant.now().plusSeconds(600));

        handler.logout(request, response, authentication);

        verify(authTokenService).blacklistJti(eq("jti-1"), any(Duration.class));
        verify(authTokenService).deleteRefreshTokenByUserId(userId);
        verify(authTokenService).deleteAccessJtiByUserId(userId);
    }

    @Test
    @DisplayName("성공: 이미 만료된 토큰이면 블랙리스트 등록 없이 리프레시/액세스 토큰만 삭제한다")
    void logout_expiredToken_skipsBlacklistButDeletesTokens() {
        UUID userId = UUID.randomUUID();
        JwtClaims claims = JwtClaims.builder().userId(userId).tokenId("jti-1").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer raw-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.getExpiration("raw-token")).thenReturn(Instant.now().minusSeconds(600));

        handler.logout(request, response, authentication);

        verify(authTokenService, never()).blacklistJti(anyString(), any(Duration.class));
        verify(authTokenService).deleteRefreshTokenByUserId(userId);
        verify(authTokenService).deleteAccessJtiByUserId(userId);
    }

    @Test
    @DisplayName("실패: 인증 정보가 없으면 아무 작업도 수행하지 않는다")
    void logout_noAuthentication_doesNothing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.logout(request, response, null);

        verifyNoInteractions(authTokenService);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("실패: principal이 JwtClaims가 아니면 아무 작업도 수행하지 않는다")
    void logout_principalNotJwtClaims_doesNothing() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("woody@mopl.io", null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.logout(request, response, authentication);

        verifyNoInteractions(authTokenService);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("실패: Authorization 헤더가 없으면 아무 작업도 수행하지 않는다")
    void logout_missingAuthorizationHeader_doesNothing() {
        JwtClaims claims = JwtClaims.builder().userId(UUID.randomUUID()).tokenId("jti-1").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.logout(request, response, authentication);

        verifyNoInteractions(authTokenService);
        verifyNoInteractions(jwtProvider);
    }
}
