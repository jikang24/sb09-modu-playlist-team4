package com.mopl.domain.auth.dto;

import java.time.Duration;

public record RefreshResult(
        JwtDto jwtDto,
        String refreshToken,
        Duration refreshTokenTtl
) {
}
