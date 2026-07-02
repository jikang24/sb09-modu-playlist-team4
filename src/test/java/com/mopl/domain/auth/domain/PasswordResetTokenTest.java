package com.mopl.domain.auth.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordResetToken 테스트")
class PasswordResetTokenTest {

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 팩토리 메서드로 토큰을 생성한다")
    void create() {
        UUID userId = UUID.randomUUID();
        String tempPassword = "tempPass123!@#";
        Instant expiresAt = Instant.now().plus(3, ChronoUnit.MINUTES);

        PasswordResetToken token = PasswordResetToken.create(userId, tempPassword, expiresAt);

        assertNotNull(token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals(tempPassword, token.getTemporaryPassword());
        assertEquals(expiresAt, token.getExpiresAt());
        assertFalse(token.isUsed());
    }

    @Test
    @DisplayName("성공: 만료되지 않은 토큰이 유효하다")
    void isExpired_NotExpired() {
        Instant futureExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(futureExpiry)
                .used(false)
                .build();

        assertFalse(token.isExpired());
    }

    @Test
    @DisplayName("실패: 만료된 토큰은 유효하지 않다")
    void isExpired_Expired() {
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(pastExpiry)
                .used(false)
                .build();

        assertTrue(token.isExpired());
    }

    @Test
    @DisplayName("성공: 사용되지 않고 만료되지 않은 토큰이 유효하다")
    void isValid_ValidToken() {
        Instant futureExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(futureExpiry)
                .used(false)
                .build();

        assertTrue(token.isValid());
    }

    @Test
    @DisplayName("실패: 만료된 토큰은 유효하지 않다")
    void isValid_ExpiredToken() {
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(pastExpiry)
                .used(false)
                .build();

        assertFalse(token.isValid());
    }

    @Test
    @DisplayName("실패: 사용된 토큰은 유효하지 않다")
    void isValid_UsedToken() {
        Instant futureExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(futureExpiry)
                .used(true)
                .build();

        assertFalse(token.isValid());
    }

    @Test
    @DisplayName("실패: 사용되고 만료된 토큰은 유효하지 않다")
    void isValid_BothExpiredAndUsed() {
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(pastExpiry)
                .used(true)
                .build();

        assertFalse(token.isValid());
    }

    @Test
    @DisplayName("성공: 토큰을 사용 상태로 마킹한다")
    void markUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        assertFalse(token.isUsed());
        token.markUsed();
        assertTrue(token.isUsed());
    }

    @Test
    @DisplayName("성공: 토큰을 사용 상태로 마킹하면 유효하지 않게 된다")
    void markUsed_InvalidatesToken() {
        Instant futureExpiry = Instant.now().plus(10, ChronoUnit.MINUTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .temporaryPassword("tempPass")
                .expiresAt(futureExpiry)
                .used(false)
                .build();

        assertTrue(token.isValid());
        token.markUsed();
        assertFalse(token.isValid());
    }
}
