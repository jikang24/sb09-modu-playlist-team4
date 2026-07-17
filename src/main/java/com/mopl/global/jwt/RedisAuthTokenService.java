package com.mopl.global.jwt;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 키 설계 (다중 인스턴스에서 공유되어야 하므로 인메모리 대신 Redis에 저장)
 *
 *  auth:blacklist:{jti}          String "1"          - 폐기된 액세스 토큰 jti (TTL=남은 만료 시간)
 *  auth:refresh:user:{userId}    String refreshToken  - 유저별 현재 유효한 리프레시 토큰 (TTL=ttl)
 *  auth:refresh:token:{token}    String userId         - 리프레시 토큰 -> 유저 역인덱스 (TTL=ttl)
 *  auth:access-jti:user:{userId} String jti            - 유저별 현재 유효한 액세스 토큰 jti (TTL=만료까지 남은 시간, 기존 로그인 강제 로그아웃용)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthTokenService implements AuthTokenService {

  private static final String BLACKLIST_PREFIX = "auth:blacklist:";
  private static final String REFRESH_USER_PREFIX = "auth:refresh:user:";
  private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:token:";
  private static final String ACCESS_JTI_PREFIX = "auth:access-jti:user:";

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
    String oldToken = redisTemplate.opsForValue().get(refreshUserKey(userId));
    if (oldToken != null) {
      redisTemplate.delete(refreshTokenKey(oldToken));
      log.debug("기존 리프레시 토큰 제거 - userId: {}", userId);
    }

    redisTemplate.opsForValue().set(refreshUserKey(userId), refreshToken, ttl);
    redisTemplate.opsForValue().set(refreshTokenKey(refreshToken), userId.toString(), ttl);
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
    } catch (RedisSystemException e) {
      log.error("강제 로그아웃 처리 실패 (Redis 오류) - userId: {}", userId, e);
      throw new MoplException(ErrorCode.AUTH_REDIS_UNAVAILABLE);
    } catch (RuntimeException e) {
      log.error("강제 로그아웃 처리 실패 (예상치 못한 오류) - userId: {}", userId, e);
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
