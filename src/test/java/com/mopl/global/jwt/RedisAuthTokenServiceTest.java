package com.mopl.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import com.mopl.global.exception.MoplException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAuthTokenService 테스트")
class RedisAuthTokenServiceTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private RedisAuthTokenService service;

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service = new RedisAuthTokenService(redisTemplate);
  }

  private String blacklistKey(String jti) {
    return "auth:blacklist:" + jti;
  }

  private String refreshUserKey(UUID userId) {
    return "auth:refresh:user:" + userId;
  }

  private String refreshTokenKey(String refreshToken) {
    return "auth:refresh:token:" + refreshToken;
  }

  private String accessJtiKey(UUID userId) {
    return "auth:access-jti:user:" + userId;
  }

  @Nested
  @DisplayName("블랙리스트")
  class Blacklist {

    @Test
    @DisplayName("성공: jti를 TTL과 함께 Redis에 등록한다")
    void blacklistJti_setsWithTtl() {
      service.blacklistJti("jti-1", Duration.ofMinutes(5));

      then(valueOperations).should().set(blacklistKey("jti-1"), "1", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("실패: 이미 만료된 TTL이면 Redis에 등록하지 않는다")
    void blacklistJti_nonPositiveTtl_doesNotSet() {
      service.blacklistJti("jti-expired", Duration.ofMillis(-1000));

      then(valueOperations).should(never()).set(eq(blacklistKey("jti-expired")), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("성공: 키가 존재하면 블랙리스트된 것으로 판단한다")
    void isBlacklistedJti_keyExists_returnsTrue() {
      given(redisTemplate.hasKey(blacklistKey("jti-1"))).willReturn(true);

      assertThat(service.isBlacklistedJti("jti-1")).isTrue();
    }

    @Test
    @DisplayName("실패: 키가 없으면 블랙리스트되지 않은 것으로 판단한다")
    void isBlacklistedJti_keyMissing_returnsFalse() {
      given(redisTemplate.hasKey(blacklistKey("unknown-jti"))).willReturn(false);

      assertThat(service.isBlacklistedJti("unknown-jti")).isFalse();
    }

    @Test
    @DisplayName("실패: Redis 조회 중 오류가 발생하면 fail-open으로 블랙리스트되지 않은 것으로 간주한다")
    void isBlacklistedJti_redisError_failsOpen() {
      given(redisTemplate.hasKey(blacklistKey("jti-1")))
          .willThrow(new DataAccessResourceFailureException("redis down"));

      assertThat(service.isBlacklistedJti("jti-1")).isFalse();
    }
  }

  @Nested
  @DisplayName("리프레시 토큰")
  class RefreshToken {

    @Test
    @DisplayName("성공: 저장한 리프레시 토큰으로 userId를 조회한다")
    void findUserIdByRefreshToken_found() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshTokenKey("refresh-token-1"))).willReturn(userId.toString());

      Optional<UUID> result = service.findUserIdByRefreshToken("refresh-token-1");

      assertThat(result).contains(userId);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 리프레시 토큰이면 빈 값을 반환한다")
    void findUserIdByRefreshToken_notFound() {
      given(valueOperations.get(refreshTokenKey("unknown-token"))).willReturn(null);

      assertThat(service.findUserIdByRefreshToken("unknown-token")).isEmpty();
    }

    @Test
    @DisplayName("성공: 기존 토큰이 없으면 새 리프레시 토큰 매핑 2개만 저장한다")
    void saveRefreshToken_noExistingToken() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshUserKey(userId))).willReturn(null);

      service.saveRefreshToken(userId, "new-token", Duration.ofDays(7));

      then(valueOperations).should().set(refreshUserKey(userId), "new-token", Duration.ofDays(7));
      then(valueOperations).should().set(refreshTokenKey("new-token"), userId.toString(), Duration.ofDays(7));
      then(redisTemplate).should(never()).delete(any(String.class));
    }

    @Test
    @DisplayName("성공: 기존 토큰이 있으면 그 역인덱스 키를 지우고 새 토큰을 저장한다")
    void saveRefreshToken_replacesOldToken() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshUserKey(userId))).willReturn("old-token");

      service.saveRefreshToken(userId, "new-token", Duration.ofDays(7));

      then(redisTemplate).should().delete(refreshTokenKey("old-token"));
      then(valueOperations).should().set(refreshUserKey(userId), "new-token", Duration.ofDays(7));
      then(valueOperations).should().set(refreshTokenKey("new-token"), userId.toString(), Duration.ofDays(7));
    }

    @Test
    @DisplayName("성공: 토큰으로 조회되면 토큰-유저 매핑을 모두 삭제한다")
    void deleteRefreshToken_found() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshTokenKey("token-to-delete"))).willReturn(userId.toString());

      service.deleteRefreshToken("token-to-delete");

      then(redisTemplate).should().delete(refreshTokenKey("token-to-delete"));
      then(redisTemplate).should().delete(refreshUserKey(userId));
    }

    @Test
    @DisplayName("성공: 존재하지 않는 토큰을 삭제해도 아무 키도 지우지 않는다")
    void deleteRefreshToken_notFound_noDelete() {
      given(valueOperations.get(refreshTokenKey("nonexistent-token"))).willReturn(null);

      service.deleteRefreshToken("nonexistent-token");

      then(redisTemplate).should(never()).delete(any(String.class));
    }

    @Test
    @DisplayName("성공: userId로 조회되면 유저-토큰 매핑을 모두 삭제한다")
    void deleteRefreshTokenByUserId_found() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshUserKey(userId))).willReturn("token-1");

      service.deleteRefreshTokenByUserId(userId);

      then(redisTemplate).should().delete(refreshUserKey(userId));
      then(redisTemplate).should().delete(refreshTokenKey("token-1"));
    }

    @Test
    @DisplayName("성공: 등록된 적 없는 userId로 삭제해도 아무 키도 지우지 않는다")
    void deleteRefreshTokenByUserId_notFound_noDelete() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(refreshUserKey(userId))).willReturn(null);

      service.deleteRefreshTokenByUserId(userId);

      then(redisTemplate).should(never()).delete(any(String.class));
    }

    @Test
    @DisplayName("성공: 스크립트가 1을 반환하면 교체 성공으로 판단한다")
    void rotateRefreshToken_scriptReturnsOne_returnsTrue() {
      UUID userId = UUID.randomUUID();
      given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
          .willReturn(1L);

      boolean result = service.rotateRefreshToken(userId, "old-token", "new-token", Duration.ofDays(7));

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("실패: 스크립트가 0을 반환하면 이미 교체된 것으로 보고 실패를 반환한다")
    void rotateRefreshToken_scriptReturnsZero_returnsFalse() {
      UUID userId = UUID.randomUUID();
      given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
          .willReturn(0L);

      boolean result = service.rotateRefreshToken(userId, "old-token", "new-token", Duration.ofDays(7));

      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("액세스 토큰 jti")
  class AccessJti {

    @Test
    @DisplayName("성공: 만료 시각까지 남은 시간을 TTL로 저장한다")
    void saveAccessJti_positiveTtl_sets() {
      UUID userId = UUID.randomUUID();
      Instant expiresAt = Instant.now().plusSeconds(1800);

      service.saveAccessJti(userId, "access-jti-1", expiresAt);

      then(valueOperations).should().set(eq(accessJtiKey(userId)), eq("access-jti-1"), any(Duration.class));
    }

    @Test
    @DisplayName("실패: 이미 만료된 시각이면 저장하지 않는다")
    void saveAccessJti_alreadyExpired_doesNotSet() {
      UUID userId = UUID.randomUUID();

      service.saveAccessJti(userId, "expired-jti", Instant.now().minusSeconds(10));

      then(valueOperations).should(never()).set(eq(accessJtiKey(userId)), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("성공: 저장된 jti와 Redis TTL로부터 만료 시각을 재구성해 반환한다")
    void findAccessJtiByUserId_found() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willReturn("access-jti-1");
      given(redisTemplate.getExpire(accessJtiKey(userId))).willReturn(1800L);

      Optional<AccessJtiEntry> result = service.findAccessJtiByUserId(userId);

      assertThat(result).isPresent();
      assertThat(result.get().jti()).isEqualTo("access-jti-1");
      assertThat(result.get().expiresAt()).isCloseTo(Instant.now().plusSeconds(1800), within(2, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("실패: 저장된 적 없는 userId는 빈 값을 반환한다")
    void findAccessJtiByUserId_notFound() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willReturn(null);

      assertThat(service.findAccessJtiByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("성공: 액세스 jti 키를 삭제한다")
    void deleteAccessJtiByUserId() {
      UUID userId = UUID.randomUUID();

      service.deleteAccessJtiByUserId(userId);

      then(redisTemplate).should().delete(accessJtiKey(userId));
    }
  }

  @Nested
  @DisplayName("강제 로그아웃")
  class ForceLogout {

    @Test
    @DisplayName("성공: 살아있는 액세스 토큰이 있으면 블랙리스트 등록 후 access-jti/refresh를 모두 삭제한다")
    void forceLogoutByUserId_withActiveAccessToken_blacklistsAndDeletesAll() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willReturn("access-jti-1");
      given(redisTemplate.getExpire(accessJtiKey(userId))).willReturn(1800L);
      given(valueOperations.get(refreshUserKey(userId))).willReturn("refresh-token-1");

      service.forceLogoutByUserId(userId);

      then(valueOperations).should().set(eq(blacklistKey("access-jti-1")), eq("1"), any(Duration.class));
      then(redisTemplate).should().delete(accessJtiKey(userId));
      then(redisTemplate).should().delete(refreshUserKey(userId));
      then(redisTemplate).should().delete(refreshTokenKey("refresh-token-1"));
    }

    @Test
    @DisplayName("실패: jti는 남아있지만 이미 만료된 상태면 블랙리스트 등록 없이 삭제만 진행한다")
    void forceLogoutByUserId_expiredAccessToken_skipsBlacklist() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willReturn("access-jti-1");
      given(redisTemplate.getExpire(accessJtiKey(userId))).willReturn(-1L);
      given(valueOperations.get(refreshUserKey(userId))).willReturn(null);

      service.forceLogoutByUserId(userId);

      then(valueOperations).should(never()).set(any(String.class), any(String.class), any(Duration.class));
      then(redisTemplate).should().delete(accessJtiKey(userId));
    }

    @Test
    @DisplayName("성공: 저장된 액세스 토큰이 없으면 블랙리스트 등록 없이 access-jti/refresh 삭제만 시도한다")
    void forceLogoutByUserId_withoutAccessToken_skipsBlacklist() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willReturn(null);
      given(valueOperations.get(refreshUserKey(userId))).willReturn(null);

      service.forceLogoutByUserId(userId);

      then(valueOperations).should(never()).set(any(String.class), any(String.class), any(Duration.class));
      then(redisTemplate).should().delete(accessJtiKey(userId));
    }

    @Test
    @DisplayName("성공: Redis 접근 예외가 발생해도 예외를 던지지 않고 로그만 남기고 넘어간다 (fail-soft)")
    void forceLogoutByUserId_dataAccessExceptionThrown_failsSoft() {
      UUID userId = UUID.randomUUID();

      given(redisTemplate.delete(accessJtiKey(userId)))
              .willThrow(new DataAccessResourceFailureException("redis connection failed"));

      assertThatCode(() -> service.forceLogoutByUserId(userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("실패: Redis와 무관한 런타임 예외는 감추지 않고 그대로 전파한다")
    void forceLogoutByUserId_unexpectedRuntimeException_propagatesAsIs() {
      UUID userId = UUID.randomUUID();
      given(valueOperations.get(accessJtiKey(userId))).willThrow(new IllegalStateException("unexpected bug"));

      assertThatThrownBy(() -> service.forceLogoutByUserId(userId))
          .isInstanceOf(IllegalStateException.class)
          .isNotInstanceOf(MoplException.class);
    }
  }
}
