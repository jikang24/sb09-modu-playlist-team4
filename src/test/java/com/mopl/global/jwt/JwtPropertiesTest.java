package com.mopl.global.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProperties 테스트")
class JwtPropertiesTest {

    @Test
    @DisplayName("성공: 생성자로 전달한 값을 그대로 보관한다")
    void holdsConstructorValues() {
        JwtProperties properties = new JwtProperties("secret-value", 1_800_000L, 604_800_000L);

        assertThat(properties.getSecret()).isEqualTo("secret-value");
        assertThat(properties.getAccessTokenExpiryMs()).isEqualTo(1_800_000L);
        assertThat(properties.getRefreshTokenExpiryMs()).isEqualTo(604_800_000L);
    }
}
