package com.mopl.domain.auth.adapter.in.oauth2;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        this.profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        return getNickname() + "_" + getProviderId() + "@kakao.com";
    }

    @Override
    public String getNickname() {
        return (String) profile.getOrDefault("nickname","kakao_user");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) profile.get("profile_image_url");
    }

    @Override
    public boolean isEmailVerified() {
        return false;
    }
}
