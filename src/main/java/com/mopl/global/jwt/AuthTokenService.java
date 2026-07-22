package com.mopl.global.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenService {
    // 블랙리스트
    void blacklistJti(String jti, Duration ttl);

    boolean isBlacklistedJti(String jti);

    // 리프레시 토큰 로직
    void saveRefreshToken(UUID userId, String refreshToken, Duration ttl);

    boolean isValidRefreshToken(UUID userId, String refreshToken);

    void deleteRefreshTokenByUserId(UUID userId);

    // 유저별 현재 액세스 토큰 jti 관리 (기존 로그인 강제 로그아웃용)
    void saveAccessJti(UUID userId, String jti, Instant expiresAt);

    Optional<AccessJtiEntry> findAccessJtiByUserId(UUID userId);

    void deleteAccessJtiByUserId(UUID userId);

    // 계정 잠금/권한 변경 등으로 인한 강제 로그아웃
    void forceLogoutByUserId(UUID userId);
}

