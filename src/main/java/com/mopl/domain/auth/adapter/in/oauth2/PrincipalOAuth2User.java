package com.mopl.domain.auth.adapter.in.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

//Kakao는 OAuth2User 반환, 계층형
public class PrincipalOAuth2User extends DefaultOAuth2User implements UserIdAware {

    private final UUID userId;

    public PrincipalOAuth2User(UUID userId, Collection<? extends GrantedAuthority> authorities,
                               Map<String, Object> attributes, String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }
}