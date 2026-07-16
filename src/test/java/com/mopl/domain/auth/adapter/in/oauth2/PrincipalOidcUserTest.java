package com.mopl.domain.auth.adapter.in.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrincipalOidcUser 테스트")
class PrincipalOidcUserTest {

    @Test
    @DisplayName("성공: userId를 보관하고 ROLE_USER 권한을 가진다")
    void holdsUserIdAndRole() {
        UUID userId = UUID.randomUUID();
        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "google-1", "email", "woody@gmail.com")
        );

        PrincipalOidcUser principal = new PrincipalOidcUser(userId, idToken);

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal).isInstanceOf(UserIdAware.class);
        assertThat(principal.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_USER");
        assertThat(principal.getIdToken()).isEqualTo(idToken);
    }
}
