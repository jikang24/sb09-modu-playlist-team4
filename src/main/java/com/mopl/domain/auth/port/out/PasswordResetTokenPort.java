package com.mopl.domain.auth.port.out;

import com.mopl.domain.auth.domain.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPort {
    Optional<PasswordResetToken> findActiveByUserId(UUID userId);

    void save(PasswordResetToken token);

    void deleteByUserId(UUID userId);

    // 기존 토큰 삭제 + 신규 토큰 저장을 하나의 트랜잭션으로 원자적으로 처리
    void replaceForUser(UUID userId, PasswordResetToken newToken);
}
