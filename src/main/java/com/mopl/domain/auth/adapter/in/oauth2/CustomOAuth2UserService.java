package com.mopl.domain.auth.adapter.in.oauth2;

import com.mopl.domain.auth.port.out.user.RegisterSocialUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.global.exception.MoplException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RegisterSocialUserPort registerSocialUserPort;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.getAttributes());

        UUID userId = resolveUserId(registrationId, userInfo);

        return new PrincipalOAuth2User(
                userId,
                oAuth2User.getAuthorities(),
                oAuth2User.getAttributes(),
                "id"
        );
    }

    private UUID resolveUserId(String registrationId, OAuth2UserInfo userInfo) {
        try {
            return registerSocialUserPort.registerOrLink(
                    SocialProvider.from(registrationId),
                    userInfo.getProviderId(),
                    userInfo.getEmail(),
                    userInfo.isEmailVerified(),
                    userInfo.getNickname(),
                    userInfo.getProfileImageUrl());
        } catch (MoplException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(e.getErrorCode().name(), e.getMessage(), null));
        }
    }
}
