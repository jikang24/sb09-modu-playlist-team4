package com.mopl.global.security.handler;

import com.mopl.domain.auth.adapter.in.oauth2.UserIdAware;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoplOAuth2LoginSuccessHandler 테스트")
class MoplOAuth2LoginSuccessHandlerTest {

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private UserAuthPort userAuthPort;

    @Mock
    private Authentication authentication;

    private MoplOAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplOAuth2LoginSuccessHandler(authTokenIssuer, userAuthPort);
        ReflectionTestUtils.setField(handler, "frontendBaseUrl", "https://mopl.io");
    }

    @Test
    @DisplayName("성공: 토큰을 발급하고 프론트엔드 홈으로 리다이렉트한다")
    void onAuthenticationSuccess_redirectsHome() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAuthInfo userAuthInfo = new UserAuthInfo(userId, Instant.now(), "woody@gmail.com", null,
                "woody", null, Role.USER, false);
        UserIdAware principal = () -> userId;
        when(authentication.getPrincipal()).thenReturn(principal);
        when(userAuthPort.findById(userId)).thenReturn(Optional.of(userAuthInfo));
        when(authTokenIssuer.issue(org.mockito.ArgumentMatchers.eq(userAuthInfo), org.mockito.ArgumentMatchers.any()))
                .thenReturn("access-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authTokenIssuer).issue(org.mockito.ArgumentMatchers.eq(userAuthInfo), org.mockito.ArgumentMatchers.any());
        assertThat(response.getRedirectedUrl()).isEqualTo("https://mopl.io/");
    }

    @Test
    @DisplayName("실패: principal에 해당하는 사용자가 없으면 예외가 발생한다")
    void onAuthenticationSuccess_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        UserIdAware principal = () -> userId;
        when(authentication.getPrincipal()).thenReturn(principal);
        when(userAuthPort.findById(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(MoplException.class)
                .extracting(e -> ((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
