package com.mopl.domain.auth.repository;

import com.mopl.domain.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
