package com.mopl.global.jwt;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenService {
    //기존 블랙리스트 로직
    void blacklist(String accessToken, Duration ttl);

    boolean isBlacklisted(String accessToken);

    // 리프레시 토큰 로직
    void saveRefreshToken(UUID userId, String refreshToken, Duration ttl);

    Optional<UUID> findUserIdByRefreshToken(String refreshToken);

    void deleteRefreshToken(String refreshToken);

    void deleteRefreshTokenByUserId(UUID userId);
}

