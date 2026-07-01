package com.mopl.domain.auth.port.out;

import com.mopl.domain.auth.domain.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPort {
    Optional<PasswordResetToken> findActiveByUserId(UUID userId);

    void save(PasswordResetToken token);

    void deleteByUserId(UUID userId);
}
