package com.mopl.domain.auth.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class PasswordResetToken {

    private UUID id;

    private UUID userId;

    private String temporaryPassword;

    private Instant expiresAt;

    private boolean used;

    public static PasswordResetToken create(UUID userId, String temporaryPassword, Instant expiresAt) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .temporaryPassword(temporaryPassword)
                .expiresAt(expiresAt)
                .used(false)
                .build();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markUsed() {
        this.used = true;
    }
}