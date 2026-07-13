package com.mopl.domain.auth.adapter.in.oauth2;

import com.mopl.domain.auth.port.out.user.RegisterSocialUserPort;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final RegisterSocialUserPort registerSocialUserPort;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oidcUser.getAttributes());

        try {
            UUID userId = registerSocialUserPort.registerOrLink(
                    SocialProvider.from(registrationId),
                    userInfo.getProviderId(),
                    userInfo.getEmail(),
                    userInfo.isEmailVerified(),
                    userInfo.getNickname(),
                    userInfo.getProfileImageUrl());

            return new PrincipalOidcUser(userId, oidcUser.getIdToken());
        } catch (MoplException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorCode().name(), e.getMessage(), null));
        }
    }
}
