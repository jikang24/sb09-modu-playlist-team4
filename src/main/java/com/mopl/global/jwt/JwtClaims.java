package com.mopl.global.jwt;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class JwtClaims {
    private final UUID userId;
    private final String email;
    private final String role;
    private final String tokenId;
}