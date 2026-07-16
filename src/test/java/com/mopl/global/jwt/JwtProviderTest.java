package com.mopl.global.jwt;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtProvider 테스트")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private UUID userId;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-for-jwt-provider-test-minimum-256-bits-long-value",
                1_800_000L,
                604_800_000L
        );
        jwtProvider = new JwtProvider(properties);
        jwtProvider.init();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 액세스 토큰을 발급하고 파싱하면 원래 정보가 그대로 복원된다")
    void generateAndParseAccessToken() {
        String token = jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        JwtClaims claims = jwtProvider.parse(token);

        assertThat(claims.getUserId()).isEqualTo(userId);
        assertThat(claims.getEmail()).isEqualTo("woody@mopl.io");
        assertThat(claims.getRole()).isEqualTo("USER");
        assertThat(claims.getTokenId()).isNotBlank();
    }

    @Test
    @DisplayName("성공: 리프레시 토큰을 발급하고 파싱할 수 있다")
    void generateAndParseRefreshToken() {
        String token = jwtProvider.generateRefreshToken(userId, "woody@mopl.io", "ADMIN");

        JwtClaims claims = jwtProvider.parse(token);

        assertThat(claims.getUserId()).isEqualTo(userId);
        assertThat(claims.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("성공: 발급될 때마다 토큰 아이디(jti)가 달라진다")
    void eachTokenHasUniqueJti() {
        String token1 = jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER");
        String token2 = jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        assertThat(jwtProvider.parse(token1).getTokenId())
                .isNotEqualTo(jwtProvider.parse(token2).getTokenId());
    }

    @Test
    @DisplayName("실패: 만료된 토큰을 파싱하면 TOKEN_EXPIRED 예외가 발생한다")
    void parse_expiredToken_throwsTokenExpired() {
        JwtProperties shortLivedProps = new JwtProperties(
                "test-secret-key-for-jwt-provider-test-minimum-256-bits-long-value",
                -1000L,
                604_800_000L
        );
        JwtProvider shortLivedProvider = new JwtProvider(shortLivedProps);
        shortLivedProvider.init();
        String expiredToken = shortLivedProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        assertThatThrownBy(() -> jwtProvider.parse(expiredToken))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("실패: 형식이 잘못된 토큰을 파싱하면 INVALID_TOKEN 예외가 발생한다")
    void parse_malformedToken_throwsInvalidToken() {
        assertThatThrownBy(() -> jwtProvider.parse("not-a-valid-jwt-token"))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("실패: 다른 비밀키로 서명된 토큰을 파싱하면 INVALID_TOKEN 예외가 발생한다")
    void parse_tokenSignedWithDifferentKey_throwsInvalidToken() {
        JwtProperties otherProps = new JwtProperties(
                "a-completely-different-secret-key-for-signature-mismatch-test-256bit",
                1_800_000L,
                604_800_000L
        );
        JwtProvider otherProvider = new JwtProvider(otherProps);
        otherProvider.init();
        String tokenFromOtherProvider = otherProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        assertThatThrownBy(() -> jwtProvider.parse(tokenFromOtherProvider))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("성공: 유효한 토큰의 만료 시각을 조회한다")
    void getExpiration_validToken() {
        Instant before = Instant.now();
        String token = jwtProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        Instant expiration = jwtProvider.getExpiration(token);

        assertThat(expiration).isAfter(before.plusMillis(1_800_000L).minusSeconds(5));
        assertThat(expiration).isBefore(before.plusMillis(1_800_000L).plusSeconds(5));
    }

    @Test
    @DisplayName("성공: 만료된 토큰이어도 만료 시각은 조회할 수 있다")
    void getExpiration_expiredToken() {
        JwtProperties shortLivedProps = new JwtProperties(
                "test-secret-key-for-jwt-provider-test-minimum-256-bits-long-value",
                -1000L,
                604_800_000L
        );
        JwtProvider shortLivedProvider = new JwtProvider(shortLivedProps);
        shortLivedProvider.init();
        String expiredToken = shortLivedProvider.generateAccessToken(userId, "woody@mopl.io", "USER");

        Instant expiration = jwtProvider.getExpiration(expiredToken);

        assertThat(expiration).isBefore(Instant.now());
    }

    @Test
    @DisplayName("성공: 남은 TTL을 계산한다")
    void calculateTtl() {
        String token = jwtProvider.generateRefreshToken(userId, "woody@mopl.io", "USER");

        Duration ttl = jwtProvider.calculateTtl(token);

        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(Duration.ofMillis(604_800_000L));
    }
}
