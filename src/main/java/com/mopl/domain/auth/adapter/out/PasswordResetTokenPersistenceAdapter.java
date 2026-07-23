package com.mopl.domain.auth.adapter.out;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenPort  {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenEntityMapper entityMapper;

    @Override
    public Optional<PasswordResetToken> findActiveByUserId(UUID userId) {
        return jpaRepository
                .findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(userId)
                .map(entityMapper::toDomain);
    }

    @Override
    public void save(PasswordResetToken token) {
        jpaRepository.save(entityMapper.toJpaEntity(token));
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void replaceForUser(UUID userId, PasswordResetToken newToken) {
        jpaRepository.deleteByUserId(userId);
        jpaRepository.save(entityMapper.toJpaEntity(newToken));
    }
}
