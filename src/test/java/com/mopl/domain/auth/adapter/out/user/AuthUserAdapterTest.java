package com.mopl.domain.auth.adapter.out.user;

import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserAdapter 테스트")
class AuthUserAdapterTest {

    @Mock
    private UserService userService;

    private AuthUserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthUserAdapter(userService);
    }

    @Test
    @DisplayName("성공: UserService에 위임하여 소셜 사용자를 등록/연동하고 userId를 반환한다")
    void registerOrLink_delegatesToUserService() {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, Instant.now(), "woody@gmail.com", "woody", null, Role.USER, false);
        when(userService.findOrCreateSocialUser(
                SocialProvider.GOOGLE, "provider-id", "woody@gmail.com", true, "woody", "https://pic.url"))
                .thenReturn(userDto);

        UUID result = adapter.registerOrLink(
                SocialProvider.GOOGLE, "provider-id", "woody@gmail.com", true, "woody", "https://pic.url");

        assertThat(result).isEqualTo(userId);
        verify(userService).findOrCreateSocialUser(
                SocialProvider.GOOGLE, "provider-id", "woody@gmail.com", true, "woody", "https://pic.url");
    }

    @Test
    @DisplayName("성공: 카카오 provider로도 동일하게 위임한다")
    void registerOrLink_kakao() {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, Instant.now(), "buzz_1@kakao.com", "buzz", null, Role.USER, false);
        when(userService.findOrCreateSocialUser(
                SocialProvider.KAKAO, "1", "buzz_1@kakao.com", false, "buzz", null))
                .thenReturn(userDto);

        UUID result = adapter.registerOrLink(SocialProvider.KAKAO, "1", "buzz_1@kakao.com", false, "buzz", null);

        assertThat(result).isEqualTo(userId);
    }
}
