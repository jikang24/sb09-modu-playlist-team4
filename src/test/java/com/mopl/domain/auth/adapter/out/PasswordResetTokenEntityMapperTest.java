package com.mopl.domain.auth.adapter.out;

import com.mopl.domain.auth.domain.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetTokenEntityMapper 테스트")
class PasswordResetTokenEntityMapperTest {

    private final PasswordResetTokenEntityMapper mapper = new PasswordResetTokenEntityMapperImpl();

    private UUID tokenId;
    private UUID userId;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        tokenId = UUID.randomUUID();
        userId = UUID.randomUUID();
        expiresAt = Instant.now().plus(3, ChronoUnit.MINUTES);
    }

    @Test
    @DisplayName("성공: 도메인을 JPA 엔티티로 변환한다")
    void toJpaEntity() throws Exception {
        PasswordResetToken domain = PasswordResetToken.builder()
                .id(tokenId)
                .userId(userId)
                .temporaryPassword("tempPass1!")
                .expiresAt(expiresAt)
                .used(false)
                .build();

        PasswordResetTokenJpaEntity entity = mapper.toJpaEntity(domain);

        assertThat(entity.getId()).isEqualTo(tokenId);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getTemporaryPassword()).isEqualTo("tempPass1!");
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.isUsed()).isFalse();
    }

    @Test
    @DisplayName("성공: JPA 엔티티를 도메인으로 변환한다")
    void toDomain() {
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.builder()
                .id(tokenId)
                .userId(userId)
                .temporaryPassword("tempPass1!")
                .expiresAt(expiresAt)
                .used(true)
                .build();

        PasswordResetToken domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(tokenId);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getTemporaryPassword()).isEqualTo("tempPass1!");
        assertThat(domain.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(domain.isUsed()).isTrue();
    }

    @Test
    @DisplayName("실패: null을 전달하면 null을 반환한다")
    void toJpaEntity_null() {
        assertThat(mapper.toJpaEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
