package com.mopl.domain.auth.adapter.out.user;

import com.mopl.domain.auth.port.out.user.RegisterSocialUserPort;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements RegisterSocialUserPort {
    private final UserService userService;

    @Override
    public UUID registerOrLink(SocialProvider provider, String providerUserId,
                               String email, boolean emailVerified,
                               String nickname, String profileImageUrl) {
        return userService.findOrCreateSocialUser(
                provider, providerUserId, email, emailVerified, nickname, profileImageUrl).id();
    }
}