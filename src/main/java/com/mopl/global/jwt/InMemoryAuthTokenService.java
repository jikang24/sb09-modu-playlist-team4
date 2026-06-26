package com.mopl.global.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class InMemoryAuthTokenService implements AuthTokenService{
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>(); //Access Token과 만료 시간 저장
    private final Map<UUID, String> userToToken = new ConcurrentHashMap<>(); //사용자 ID와 현재 유효한 Refresh Token 매핑
    private final Map<String, TokenEntry> tokenToEntry = new ConcurrentHashMap<>(); //Refresh Token과 토큰 정보(사용자 ID, 만료 시간) 저장
    private record TokenEntry(UUID userId, Instant expiresAt) {} //토큰 정보 DTO


    @Override
    public void blacklist(String accessToken, Duration ttl) {
        blacklist.put(accessToken, Instant.now().plus(ttl));
        log.info("Access token blacklisted. JWT prefix: {}, TTL: {}",accessToken.substring(0, Math.min(accessToken.length(), 5)), ttl);
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        Instant expiry = blacklist.get(accessToken);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(accessToken);
            return false;
        }
        return true;
    }

    @Override
    public void saveRefreshToken(UUID userId, String refreshToken, Duration ttl) {
        String oldToken = userToToken.remove(userId);
        if (oldToken != null) {
            tokenToEntry.remove(oldToken);
            log.debug("Removed old refresh token for user: {}", userId);
        }

        userToToken.put(userId, refreshToken);
        tokenToEntry.put(refreshToken, new TokenEntry(userId, Instant.now().plus(ttl)));
        log.info("New refresh token saved for user: {}", userId);
    }

    @Override
    public Optional<UUID> findUserIdByRefreshToken(String refreshToken) {
        TokenEntry entry = tokenToEntry.get(refreshToken);
        if (entry == null)
            return Optional.empty();

        if (entry.expiresAt().isBefore(Instant.now())) {
            deleteRefreshToken(refreshToken);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    @Override
    public void deleteRefreshToken(String refreshToken) {
        TokenEntry entry = tokenToEntry.remove(refreshToken);
        if (entry != null) {
            userToToken.remove(entry.userId());
            log.info("Refresh token deleted for user: {}", entry.userId());
        }
    }

    @Override
    public void deleteRefreshTokenByUserId(UUID userId) {
        String token = userToToken.remove(userId);
        if (token != null)
            tokenToEntry.remove(token);
    }

    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        Instant now = Instant.now();
        int beforeBlacklist = blacklist.size();
        int beforeTokens = tokenToEntry.size();

        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
        tokenToEntry.entrySet().removeIf(e -> {
            if (e.getValue().expiresAt().isBefore(now)) {
                userToToken.remove(e.getValue().userId());
                return true;
            }
            return false;
        });

        int deletedBlacklist = beforeBlacklist - blacklist.size();
        int deletedTokens = beforeTokens - tokenToEntry.size();

        // 💡 매분마다 너무 많은 로그가 찍히는 걸 방지하기 위해, 지워진 게 있을 때만 출력하거나 debug 레벨 사용
        if (deletedBlacklist > 0 || deletedTokens > 0) {
            log.info("Evicted expired tokens from memory. [Blacklist removed: {}, RefreshToken removed: {}]",
                    deletedBlacklist, deletedTokens);
        }
    }
}
