package com.mopl.domain.auth.adapter.in.oauth2;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2UserInfoFactory 테스트")
class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("성공: registrationId가 google이면 GoogleUserInfo를 생성한다")
    void of_google() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("google", Map.of("sub", "1"));

        assertThat(userInfo).isInstanceOf(GoogleUserInfo.class);
    }

    @Test
    @DisplayName("성공: registrationId 대소문자와 무관하게 카카오를 인식한다")
    void of_kakao_caseInsensitive() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("KaKao", Map.of("id", 1L));

        assertThat(userInfo).isInstanceOf(KakaoUserInfo.class);
    }

    @Test
    @DisplayName("실패: 지원하지 않는 provider면 예외가 발생한다")
    void of_unsupportedProvider() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.of("naver", Map.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));
    }
}
