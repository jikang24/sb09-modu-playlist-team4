package com.mopl.global.jwt;

import java.time.Instant;

public record AccessJtiEntry(
        String jti, Instant expiresAt
) {
}
