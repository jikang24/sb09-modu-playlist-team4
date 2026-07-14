package com.mopl.domain.user.domain;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SocialAccount 엔티티 테스트")
class SocialAccountTest {

    @Test
    @DisplayName("성공: of()로 사용자/provider/providerUserId를 채운 소셜 계정을 생성한다")
    void of_createsSocialAccount() {
        User user = User.builder()
                .name("woody")
                .email("woody@mopl.io")
                .role(Role.USER)
                .locked(false)
                .build();

        SocialAccount account = SocialAccount.of(user, SocialProvider.GOOGLE, "google-provider-id");

        assertThat(account.getUser()).isEqualTo(user);
        assertThat(account.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(account.getProviderUserId()).isEqualTo("google-provider-id");
    }

    @Test
    @DisplayName("성공: 카카오 provider로도 생성할 수 있다")
    void of_kakao() {
        User user = User.builder()
                .name("buzz")
                .email("buzz@mopl.io")
                .role(Role.USER)
                .locked(false)
                .build();

        SocialAccount account = SocialAccount.of(user, SocialProvider.KAKAO, "123456");

        assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(account.getProviderUserId()).isEqualTo("123456");
    }
}
