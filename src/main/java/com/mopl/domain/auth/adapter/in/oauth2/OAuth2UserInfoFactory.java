package com.mopl.domain.auth.adapter.in.oauth2;

import com.mopl.domain.user.dto.SocialProvider;

import java.util.Map;

public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        SocialProvider provider = SocialProvider.from(registrationId);
        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case KAKAO -> new KakaoUserInfo(attributes);
        };
    }
}