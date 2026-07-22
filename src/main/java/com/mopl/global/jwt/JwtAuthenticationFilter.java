package com.mopl.global.jwt;

import com.mopl.global.exception.MoplException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean authenticated = false;
        String token = extractToken(request);

        if (token != null) {
            try {
                JwtClaims claims = jwtProvider.parse(token);

                if (authTokenService.isBlacklistedJti(claims.getTokenId())) {
                    log.debug("블랙리스트 처리된 토큰 - jti: {}", claims.getTokenId());
                } else {
                    authenticate(claims);
                    authenticated = true;
                }
            } catch (MoplException e) {
                log.debug("JWT 검증 실패: {}", e.getMessage());
            } catch (Exception e) {
                log.error("인증 처리 중 예상치 못한 오류 발생", e);
            }
        }

        // SSE는 만료/무효화된 액세스 토큰을 그대로 들고 재연결을 시도하므로,
        // 유효한 refresh token이 있으면 이 요청 안에서 갱신해 연결이 끊기지 않게 한다.
        if (!authenticated && isSseRequest(request)) {
            trySilentRefresh(request, response);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/sse");
    }

    private void trySilentRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken == null) {
            return;
        }

        try {
            JwtClaims refreshClaims = jwtProvider.parse(refreshToken);

            if (!authTokenService.isValidRefreshToken(refreshClaims.getUserId(), refreshToken)) {
                log.debug("SSE silent refresh 불가 - 이미 폐기된 refresh token (강제 로그아웃 등)");
                return;
            }

            String newAccessToken = jwtProvider.generateAccessToken(
                    refreshClaims.getUserId(), refreshClaims.getEmail(), refreshClaims.getRole());
            String newRefreshToken = jwtProvider.generateRefreshToken(
                    refreshClaims.getUserId(), refreshClaims.getEmail(), refreshClaims.getRole());

            Duration refreshTtl = jwtProvider.calculateTtl(newRefreshToken);
            authTokenService.saveRefreshToken(refreshClaims.getUserId(), newRefreshToken, refreshTtl);

            JwtClaims newAccessClaims = jwtProvider.parse(newAccessToken);
            authTokenService.saveAccessJti(refreshClaims.getUserId(), newAccessClaims.getTokenId(),
                    jwtProvider.getExpiration(newAccessToken));

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", newRefreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(refreshTtl)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            authenticate(newAccessClaims);
            log.info("SSE 재연결 시 액세스 토큰 silent refresh 완료 - userId: {}", refreshClaims.getUserId());
        } catch (MoplException e) {
            log.debug("SSE silent refresh 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("SSE silent refresh 중 예상치 못한 오류 발생", e);
        }
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("REFRESH_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void authenticate(JwtClaims claims) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        claims, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole()))
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}