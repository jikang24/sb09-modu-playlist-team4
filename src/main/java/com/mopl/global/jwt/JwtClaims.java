package com.mopl.global.jwt;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JwtClaims {
    private final Long userId;
    private final String email;
    private final String role;
    private final String tokenId;
}