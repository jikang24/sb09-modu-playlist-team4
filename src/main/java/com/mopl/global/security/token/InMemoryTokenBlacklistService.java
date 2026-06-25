package com.mopl.global.security.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> store = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        store.put(tokenId, expiresAt);
        log.debug("토큰 블랙리스트 등록: jti={}, expiresAt={}", tokenId, expiresAt);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        Instant expiry = store.get(tokenId);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            store.remove(tokenId);
            return false;
        }
        return true;
    }

    // 10분마다 만료된 항목을 정리
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        Instant now = Instant.now();
        int before = store.size();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue()));
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("만료된 블랙리스트 토큰 {}개 제거", removed);
        }
    }
}