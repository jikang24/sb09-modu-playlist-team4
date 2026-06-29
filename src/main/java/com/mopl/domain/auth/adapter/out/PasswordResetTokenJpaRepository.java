package com.mopl.domain.auth.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {
    Optional<PasswordResetTokenJpaEntity> findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
