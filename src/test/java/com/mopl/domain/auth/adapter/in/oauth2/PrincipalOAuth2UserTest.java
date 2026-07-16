package com.mopl.domain.auth.adapter.in.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrincipalOAuth2User 테스트")
class PrincipalOAuth2UserTest {

    @Test
    @DisplayName("성공: userId와 속성을 보관하고 UserIdAware로 조회할 수 있다")
    void holdsUserIdAndAttributes() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> attributes = Map.of("id", "kakao-1");

        PrincipalOAuth2User principal = new PrincipalOAuth2User(
                userId,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal).isInstanceOf(UserIdAware.class);
        assertThat(principal.getAttributes()).isEqualTo(attributes);
        assertThat(principal.getName()).isEqualTo("kakao-1");
    }
}
