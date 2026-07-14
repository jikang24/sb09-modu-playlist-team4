package com.mopl.domain.auth.adapter.in.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KakaoUserInfo 테스트")
class KakaoUserInfoTest {

    @Test
    @DisplayName("성공: kakao_account.profile에서 닉네임과 프로필 이미지를 읽는다")
    void extractsAttributes() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "buzz");
        profile.put("profile_image_url", "https://k.kakaocdn.net/pic.png");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 987654321L);
        attributes.put("kakao_account", kakaoAccount);

        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        assertThat(userInfo.getProviderId()).isEqualTo("987654321");
        assertThat(userInfo.getNickname()).isEqualTo("buzz");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/pic.png");
        assertThat(userInfo.getEmail()).isEqualTo("buzz_987654321@kakao.com");
        assertThat(userInfo.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("실패: kakao_account가 없으면 닉네임 기본값을 사용한다")
    void kakaoAccountMissing() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 111L);

        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        assertThat(userInfo.getNickname()).isEqualTo("kakao_user");
        assertThat(userInfo.getProfileImageUrl()).isNull();
        assertThat(userInfo.getEmail()).isEqualTo("kakao_user_111@kakao.com");
    }

    @Test
    @DisplayName("실패: profile이 없으면 닉네임 기본값을 사용한다")
    void profileMissing() {
        Map<String, Object> kakaoAccount = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 222L);
        attributes.put("kakao_account", kakaoAccount);

        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        assertThat(userInfo.getNickname()).isEqualTo("kakao_user");
    }

    @Test
    @DisplayName("성공: 카카오는 이메일 인증여부를 제공하지 않으므로 항상 false다")
    void alwaysEmailNotVerified() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 333L);

        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        assertThat(userInfo.isEmailVerified()).isFalse();
    }
}
