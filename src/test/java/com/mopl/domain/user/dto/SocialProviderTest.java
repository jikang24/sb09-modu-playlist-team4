package com.mopl.domain.user.dto;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialProvider 테스트")
class SocialProviderTest {

    @ParameterizedTest(name = "registrationId={0} -> {1}")
    @CsvSource({
            "google, GOOGLE",
            "GOOGLE, GOOGLE",
            "kakao, KAKAO",
            "KaKao, KAKAO"
    })
    @DisplayName("성공: 대소문자와 무관하게 registrationId를 provider로 변환한다")
    void from_success(String registrationId, SocialProvider expected) {
        assertThat(SocialProvider.from(registrationId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("실패: 지원하지 않는 provider면 예외가 발생한다")
    void from_unsupported() {
        assertThatThrownBy(() -> SocialProvider.from("naver"))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER));
    }

    @Test
    @DisplayName("실패: 빈 문자열이면 예외가 발생한다")
    void from_blank() {
        assertThatThrownBy(() -> SocialProvider.from(""))
                .isInstanceOf(MoplException.class);
    }
}
