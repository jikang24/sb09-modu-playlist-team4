package com.mopl.domain.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "temporary_password", nullable = false)
    private String temporaryPassword;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder
    private PasswordResetToken(UUID userId, String temporaryPassword, Instant expiresAt) {
        this.userId = userId;
        this.temporaryPassword = temporaryPassword;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}