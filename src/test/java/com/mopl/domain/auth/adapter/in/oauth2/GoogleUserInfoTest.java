package com.mopl.domain.auth.adapter.in.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GoogleUserInfo 테스트")
class GoogleUserInfoTest {

    @Test
    @DisplayName("성공: attributes에서 providerId/email/nickname/profileImageUrl을 읽는다")
    void extractsAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-12345");
        attributes.put("email", "woody@gmail.com");
        attributes.put("name", "woody");
        attributes.put("picture", "https://example.com/pic.png");
        attributes.put("email_verified", true);

        GoogleUserInfo userInfo = new GoogleUserInfo(attributes);

        assertThat(userInfo.getProviderId()).isEqualTo("google-12345");
        assertThat(userInfo.getEmail()).isEqualTo("woody@gmail.com");
        assertThat(userInfo.getNickname()).isEqualTo("woody");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://example.com/pic.png");
        assertThat(userInfo.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("실패: email_verified가 false면 이메일 미인증으로 처리한다")
    void emailNotVerified() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-12345");
        attributes.put("email_verified", false);

        GoogleUserInfo userInfo = new GoogleUserInfo(attributes);

        assertThat(userInfo.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("실패: email_verified 값이 없으면 이메일 미인증으로 처리한다")
    void emailVerifiedMissing() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-12345");

        GoogleUserInfo userInfo = new GoogleUserInfo(attributes);

        assertThat(userInfo.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("실패: email_verified가 문자열 \"true\"이면 Boolean이 아니므로 미인증 처리한다")
    void emailVerifiedNonBooleanType() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-12345");
        attributes.put("email_verified", "true");

        GoogleUserInfo userInfo = new GoogleUserInfo(attributes);

        assertThat(userInfo.isEmailVerified()).isFalse();
    }
}
