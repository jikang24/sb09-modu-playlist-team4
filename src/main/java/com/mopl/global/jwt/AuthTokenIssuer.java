package com.mopl.global.jwt;

import com.mopl.global.auth.UserAuthInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;

    public String issue(UserAuthInfo user, HttpServletResponse response) {
        // 기존 로그인 세션 강제 로그아웃: 살아있는 액세스 토큰을 블랙리스트에 등록
        authTokenService.findAccessJtiByUserId(user.id()).ifPresent(entry -> {
            Duration remaining = Duration.between(Instant.now(), entry.expiresAt());
            if (!remaining.isNegative()) {
                authTokenService.blacklistJti(entry.jti(), remaining);
            }
        });
        authTokenService.deleteRefreshTokenByUserId(user.id());

        String accessToken = jwtProvider.generateAccessToken(user.id(), user.email(), user.role().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.id(), user.email(), user.role().name());

        Duration refreshTtl = jwtProvider.calculateTtl(refreshToken);
        authTokenService.saveRefreshToken(user.id(), refreshToken, refreshTtl);

        JwtClaims accessClaims = jwtProvider.parse(accessToken);
        authTokenService.saveAccessJti(user.id(), accessClaims.getTokenId(), jwtProvider.getExpiration(accessToken));

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshTtl)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return accessToken;
    }
}
