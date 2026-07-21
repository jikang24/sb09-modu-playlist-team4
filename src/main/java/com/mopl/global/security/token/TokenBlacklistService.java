package com.mopl.global.security.token;

import java.time.Instant;


public interface TokenBlacklistService {

    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}