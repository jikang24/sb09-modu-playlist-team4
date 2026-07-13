package com.mopl.domain.auth.adapter.in.oauth2;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
    boolean isEmailVerified();
}
