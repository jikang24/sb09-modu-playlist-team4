package com.mopl.domain.auth.adapter.in.oauth2;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;
import java.util.UUID;

//Google은 OidcUser 반환, flat
public class PrincipalOidcUser extends DefaultOidcUser implements UserIdAware {

    private final UUID userId;

    public PrincipalOidcUser(UUID userId, OidcIdToken idToken) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken);
        this.userId = userId;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }
}