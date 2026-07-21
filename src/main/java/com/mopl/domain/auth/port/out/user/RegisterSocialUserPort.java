package com.mopl.domain.auth.port.out.user;

import com.mopl.domain.user.dto.SocialProvider;

import java.util.UUID;

public interface RegisterSocialUserPort {
    UUID registerOrLink(SocialProvider provider, String providerUserId,
                        String email, boolean emailVerified,
                        String nickname, String profileImageUrl);
}
