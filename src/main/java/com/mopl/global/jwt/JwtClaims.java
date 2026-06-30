package com.mopl.global.jwt;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JwtClaims {
    private final UUID userId;
    private final String email;
    private final String role;
    private final String tokenId;
}
