package com.mopl.global.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 키 설계 (다중 인스턴스에서 공유되어야 하므로 인메모리 대신 Redis에 저장)
 *
 *  auth:blacklist:{jti}          String "1"          - 폐기된 액세스 토큰 jti (TTL=남은 만료 시간)
 *  auth:refresh:user:{userId}    String refreshToken  - 유저별 현재 유효한 리프레시 토큰 (TTL=ttl)
 *  auth:refresh:token:{token}    String userId         - 리프레시 토큰 -> 유저 역인덱스 (TTL=ttl)
 *  auth:access-jti:user:{userId} String jti            - 유저별 현재 유효한 액세스 토큰 jti (TTL=만료까지 남은 시간, 기존 로그인 강제 로그아웃용)
 *
 * Redis 장애 시 처리 정책
 *  - 조회(읽기): fail-open. isBlacklistedJti는 Redis 오류 시 "블랙리스트 아님"으로 간주해 로그인/요청을 계속 진행시킨다.
 *    (Redis 장애로 전체 서비스가 막히는 것을 막기 위함. 대신 blacklistJti 자체는 fail-closed로 남아있어야
 *    장애 복구 후에는 다시 정상적으로 차단된다.)
 *  - 저장/삭제(쓰기, 상태 변경): fail-closed. saveRefreshToken, saveAccessJti, forceLogoutByUserId 등은
 *    Redis 오류를 삼키지 않고 MoplException(AUTH_STORAGE_UNAVAILABLE)으로 감싸 그대로 전파한다.
 *    실패를 숨기면 강제 로그아웃/토큰 무효화가 조용히 누락될 수 있으므로,
 *    호출자가 실패를 인지하고 사용자에게 오류를 노출하거나 재시도할 수 있도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthTokenService implements AuthTokenService {

  private static final String BLACKLIST_PREFIX = "auth:blacklist:";
  private static final String REFRESH_USER_PREFIX = "auth:refresh:user:";
  private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:token:";
  private static final String ACCESS_JTI_PREFIX = "auth:access-jti:user:";

  // 조회(GET) -> 기존 토큰 역인덱스 삭제(DEL) -> 신규 저장(SET x2)을 한 번의 Redis 호출로 원자화한다.
  // 그렇지 않으면 로그인/재발급 요청이 동시에 들어왔을 때 서로의 저장 결과를 덮어 이미 무효화됐어야 할 리프레시 토큰이 잠시 함께 살아있을 수 있다.
  private static final RedisScript<Long> SAVE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
      local oldToken = redis.call('GET', KEYS[1])
      local deleted = 0
      if oldToken then
          deleted = redis.call('DEL', ARGV[4] .. oldToken)
      end
      redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
      redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
      return deleted
      """, Long.class);

  private final StringRedisTemplate redisTemplate;

  @Override
  public void blacklistJti(String jti, Duration ttl) {
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(blacklistKey(jti), "1", ttl);
    log.info("액세스 토큰 블랙리스트 등록 - jti 앞 8자: {}, TTL: {}",
        jti.substring(0, Math.min(jti.length(), 8)), ttl);
  }

  @Override
  public boolean isBlacklistedJti(String jti) {
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
    } catch (Exception e) {
      log.error("Redis 블랙리스트 조회 실패", e);
      return false;  // 에러 나면 "블랙리스트 아님"으로 처리하고 로그인 계속 진행
    }
  }

  @Override
  public void saveRefreshToken(UUID userId, String refreshToken, Duration ttl) {
    Long deleted = redisTemplate.execute(
        SAVE_REFRESH_TOKEN_SCRIPT,
        List.of(refreshUserKey(userId), refreshTokenKey(refreshToken)),
        refreshToken, userId.toString(), String.valueOf(ttl.toSeconds()), REFRESH_TOKEN_PREFIX);

    if (deleted != null && deleted > 0) {
      log.debug("기존 리프레시 토큰 제거 - userId: {}", userId);
    }
    log.info("새 리프레시 토큰 저장 - userId: {}", userId);
  }

  @Override
  public Optional<UUID> findUserIdByRefreshToken(String refreshToken) {
    String userId = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
    return Optional.ofNullable(userId).map(UUID::fromString);
  }

  @Override
  public void deleteRefreshToken(String refreshToken) {
    String userId = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
    if (userId == null) {
      return;
    }
    redisTemplate.delete(refreshTokenKey(refreshToken));
    redisTemplate.delete(refreshUserKey(UUID.fromString(userId)));
    log.info("리프레시 토큰 삭제 - userId: {}", userId);
  }

  @Override
  public void deleteRefreshTokenByUserId(UUID userId) {
    String token = redisTemplate.opsForValue().get(refreshUserKey(userId));
    if (token == null) {
      return;
    }
    redisTemplate.delete(refreshUserKey(userId));
    redisTemplate.delete(refreshTokenKey(token));
  }

  @Override
  public void saveAccessJti(UUID userId, String jti, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(accessJtiKey(userId), jti, ttl);
  }

  @Override
  public Optional<AccessJtiEntry> findAccessJtiByUserId(UUID userId) {
    String key = accessJtiKey(userId);
    String jti = redisTemplate.opsForValue().get(key);
    if (jti == null) {
      return Optional.empty();
    }

    Long remainingSeconds = redisTemplate.getExpire(key);
    Instant expiresAt = Instant.now().plusSeconds(remainingSeconds != null ? remainingSeconds : 0);
    return Optional.of(new AccessJtiEntry(jti, expiresAt));
  }

  @Override
  public void deleteAccessJtiByUserId(UUID userId) {
    redisTemplate.delete(accessJtiKey(userId));
  }

  @Override
  public void forceLogoutByUserId(UUID userId) {
    try {
      findAccessJtiByUserId(userId).ifPresent(entry -> {
        Duration remaining = Duration.between(Instant.now(), entry.expiresAt());
        if (!remaining.isNegative()) {
          blacklistJti(entry.jti(), remaining);
        }
      });
      deleteAccessJtiByUserId(userId);
      deleteRefreshTokenByUserId(userId);
      log.info("강제 로그아웃 처리 완료 - userId: {}", userId);
    } catch (DataAccessException e) {
      log.error("강제 로그아웃 중 Redis 오류 발생 - userId: {}", userId, e);
      throw new MoplException(ErrorCode.AUTH_STORAGE_UNAVAILABLE);
    }
  }

  private String blacklistKey(String jti) {
    return BLACKLIST_PREFIX + jti;
  }

  private String refreshUserKey(UUID userId) {
    return REFRESH_USER_PREFIX + userId;
  }

  private String refreshTokenKey(String refreshToken) {
    return REFRESH_TOKEN_PREFIX + refreshToken;
  }

  private String accessJtiKey(UUID userId) {
    return ACCESS_JTI_PREFIX + userId;
  }
}
