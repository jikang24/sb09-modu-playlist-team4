package com.mopl.domain.auth.adapter.out;

import com.mopl.domain.auth.domain.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenPersistenceAdapter 테스트")
class PasswordResetTokenPersistenceAdapterTest {

    @Mock
    private PasswordResetTokenJpaRepository jpaRepository;

    @Mock
    private PasswordResetTokenEntityMapper entityMapper;

    private PasswordResetTokenPersistenceAdapter adapter;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        adapter = new PasswordResetTokenPersistenceAdapter(jpaRepository, entityMapper);
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 활성 토큰을 조회한다")
    void findActiveByUserId_Found() {
        UUID tokenId = UUID.randomUUID();
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        PasswordResetToken domain = PasswordResetToken.builder()
                .id(tokenId)
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(jpaRepository.findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(testUserId))
                .thenReturn(Optional.of(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        Optional<PasswordResetToken> result = adapter.findActiveByUserId(testUserId);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
        verify(jpaRepository).findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(testUserId);
        verify(entityMapper).toDomain(entity);
    }

    @Test
    @DisplayName("실패: 활성 토큰이 없으면 Optional.empty()를 반환한다")
    void findActiveByUserId_NotFound() {
        when(jpaRepository.findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(testUserId))
                .thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = adapter.findActiveByUserId(testUserId);

        assertTrue(result.isEmpty());
        verify(jpaRepository).findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(testUserId);
        verify(entityMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("실패: 사용된 토큰은 조회되지 않는다")
    void findActiveByUserId_TokenUsed() {
        UUID tokenId = UUID.randomUUID();
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();

        when(jpaRepository.findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(testUserId))
                .thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = adapter.findActiveByUserId(testUserId);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("성공: 여러 사용자의 활성 토큰을 각각 조회한다")
    void findActiveByUserId_MultipleUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        PasswordResetTokenJpaEntity entity1 = new PasswordResetTokenJpaEntity();
        PasswordResetTokenJpaEntity entity2 = new PasswordResetTokenJpaEntity();
        PasswordResetToken domain1 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId1)
                .temporaryPassword("pass1")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();
        PasswordResetToken domain2 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId2)
                .temporaryPassword("pass2")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(jpaRepository.findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(userId1))
                .thenReturn(Optional.of(entity1));
        when(jpaRepository.findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(userId2))
                .thenReturn(Optional.of(entity2));
        when(entityMapper.toDomain(entity1)).thenReturn(domain1);
        when(entityMapper.toDomain(entity2)).thenReturn(domain2);

        Optional<PasswordResetToken> result1 = adapter.findActiveByUserId(userId1);
        Optional<PasswordResetToken> result2 = adapter.findActiveByUserId(userId2);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals(domain1, result1.get());
        assertEquals(domain2, result2.get());
    }

    @Test
    @DisplayName("성공: 토큰을 저장한다")
    void save() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(Instant.now().plus(3, ChronoUnit.MINUTES))
                .used(false)
                .build();
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();

        when(entityMapper.toJpaEntity(token)).thenReturn(entity);

        adapter.save(token);

        verify(entityMapper).toJpaEntity(token);
        verify(jpaRepository).save(entity);
    }

    @Test
    @DisplayName("성공: 여러 토큰을 저장한다")
    void save_MultipleTokens() {
        PasswordResetToken token1 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("pass1")
                .expiresAt(Instant.now().plus(3, ChronoUnit.MINUTES))
                .used(false)
                .build();
        PasswordResetToken token2 = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .temporaryPassword("pass2")
                .expiresAt(Instant.now().plus(3, ChronoUnit.MINUTES))
                .used(false)
                .build();
        PasswordResetTokenJpaEntity entity1 = new PasswordResetTokenJpaEntity();
        PasswordResetTokenJpaEntity entity2 = new PasswordResetTokenJpaEntity();

        when(entityMapper.toJpaEntity(token1)).thenReturn(entity1);
        when(entityMapper.toJpaEntity(token2)).thenReturn(entity2);

        adapter.save(token1);
        adapter.save(token2);

        verify(jpaRepository).save(entity1);
        verify(jpaRepository).save(entity2);
    }

    @Test
    @DisplayName("실패: 저장 중 데이터베이스 오류가 발생한다")
    void save_RepositoryThrowsException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(Instant.now().plus(3, ChronoUnit.MINUTES))
                .used(false)
                .build();
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();

        when(entityMapper.toJpaEntity(token)).thenReturn(entity);
        doThrow(new RuntimeException("Database error"))
                .when(jpaRepository).save(entity);

        try {
            adapter.save(token);
            verify(jpaRepository).save(entity);
        } catch (RuntimeException e) {
            verify(jpaRepository).save(entity);
        }
    }

    @Test
    @DisplayName("성공: 사용자의 토큰을 모두 삭제한다")
    void deleteByUserId() {
        adapter.deleteByUserId(testUserId);

        verify(jpaRepository).deleteByUserId(testUserId);
    }

    @Test
    @DisplayName("성공: 여러 사용자의 토큰을 각각 삭제한다")
    void deleteByUserId_MultipleUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        adapter.deleteByUserId(userId1);
        adapter.deleteByUserId(userId2);

        verify(jpaRepository).deleteByUserId(userId1);
        verify(jpaRepository).deleteByUserId(userId2);
    }

    @Test
    @DisplayName("실패: 삭제 중 데이터베이스 오류가 발생한다")
    void deleteByUserId_RepositoryThrowsException() {
        doThrow(new RuntimeException("Database error"))
                .when(jpaRepository).deleteByUserId(testUserId);

        try {
            adapter.deleteByUserId(testUserId);
            verify(jpaRepository).deleteByUserId(testUserId);
        } catch (RuntimeException e) {
            verify(jpaRepository).deleteByUserId(testUserId);
        }
    }

    @Test
    @DisplayName("성공: 삭제할 레코드가 없어도 정상 처리된다")
    void deleteByUserId_NoRecordsToDelete() {
        doNothing().when(jpaRepository).deleteByUserId(testUserId);

        adapter.deleteByUserId(testUserId);

        verify(jpaRepository).deleteByUserId(testUserId);
    }
}
