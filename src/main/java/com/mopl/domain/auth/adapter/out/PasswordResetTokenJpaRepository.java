package com.mopl.domain.auth.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {
    Optional<PasswordResetTokenJpaEntity> findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(UUID userId);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
}
