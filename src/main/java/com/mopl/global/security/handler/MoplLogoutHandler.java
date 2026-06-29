package com.mopl.global.security.handler;

import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoplLogoutHandler implements LogoutHandler {

    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String rawToken = extractToken(request);
        if (rawToken == null || authentication == null) {
            return;
        }

        JwtClaims claims = (JwtClaims) authentication.getPrincipal();

        Duration remaining = Duration.between(Instant.now(), jwtProvider.getExpiration(rawToken));
        if (!remaining.isNegative()) {
            authTokenService.blacklistJti(claims.getTokenId(), remaining);
        }

        authTokenService.deleteRefreshTokenByUserId(claims.getUserId());
        log.info("로그아웃 성공 - userId: {}", claims.getUserId());
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
