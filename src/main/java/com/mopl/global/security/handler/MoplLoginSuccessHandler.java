package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import com.mopl.global.response.ApiResponse;
import com.mopl.global.security.userdetails.MoplUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class MoplLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        MoplUserDetails userDetails = (MoplUserDetails) authentication.getPrincipal();
        UserAuthInfo user = userDetails.getUserAuthInfo();

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
                .path("/api/auth")
                .maxAge(refreshTtl)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());
        log.info("로그인 성공 - userId: {}", user.id());

        UserDto userDto = new UserDto(
                user.id(), user.createdAt(), user.email(),
                user.name(), user.profileImageUrl(), user.role(), user.locked()
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new JwtDto(userDto, accessToken));
    }
}
