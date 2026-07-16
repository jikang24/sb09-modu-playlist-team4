package com.mopl.global.security.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryTokenBlacklistService 테스트")
class InMemoryTokenBlacklistServiceTest {

    private InMemoryTokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryTokenBlacklistService();
    }

    @Test
    @DisplayName("성공: 등록된 적 없는 토큰은 revoked 상태가 아니다")
    void isRevoked_notRegistered_returnsFalse() {
        assertThat(service.isRevoked("unknown-jti")).isFalse();
    }

    @Test
    @DisplayName("성공: 등록한 토큰은 만료 전까지 revoked 상태다")
    void revoke_thenIsRevoked_returnsTrue() {
        service.revoke("jti-1", Instant.now().plusSeconds(600));

        assertThat(service.isRevoked("jti-1")).isTrue();
    }

    @Test
    @DisplayName("실패: 만료 시각이 지난 토큰은 revoked 상태가 아니며 저장소에서 제거된다")
    void isRevoked_expiredEntry_returnsFalseAndEvicts() {
        service.revoke("jti-expired", Instant.now().minusSeconds(1));

        assertThat(service.isRevoked("jti-expired")).isFalse();
        assertThat(service.isRevoked("jti-expired")).isFalse();
    }

    @Test
    @DisplayName("성공: evictExpired 호출 시 만료된 항목만 제거된다")
    void evictExpired_removesOnlyExpiredEntries() {
        service.revoke("jti-expired", Instant.now().minusSeconds(1));
        service.revoke("jti-valid", Instant.now().plusSeconds(600));

        service.evictExpired();

        assertThat(service.isRevoked("jti-expired")).isFalse();
        assertThat(service.isRevoked("jti-valid")).isTrue();
    }
}
