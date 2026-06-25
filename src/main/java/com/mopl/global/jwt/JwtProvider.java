package com.mopl.global.jwt;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties props;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getEncoder().encode(
                props.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String email, String role) {
        return buildToken(userId, email, role, props.getAccessTokenExpiryMs());
    }

    public String generateRefreshToken(Long userId, String email, String role) {
        return buildToken(userId, email, role, props.getRefreshTokenExpiryMs());
    }

    // 토큰 파싱, 검증
    public JwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return JwtClaims.builder()
                    .userId(Long.valueOf(claims.getSubject()))
                    .email(claims.get("email", String.class))
                    .role(claims.get("role", String.class))
                    .tokenId(claims.getId())
                    .build();

        } catch (ExpiredJwtException e) {
            throw new MoplException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new MoplException(ErrorCode.INVALID_TOKEN);
        }
    }

    //만료 시각 추출
    public Instant getExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .toInstant();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration().toInstant();
        }
    }

   //토큰 생성
    private String buildToken(Long userId, String email, String role, long expiryMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .signWith(key)
                .compact();
    }
}