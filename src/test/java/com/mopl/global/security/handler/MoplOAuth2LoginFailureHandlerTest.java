package com.mopl.global.security.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoplOAuth2LoginFailureHandler 테스트")
class MoplOAuth2LoginFailureHandlerTest {

    private MoplOAuth2LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplOAuth2LoginFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendBaseUrl", "https://mopl.io");
    }

    @Test
    @DisplayName("성공: OAuth2AuthenticationException이면 error 코드를 쿼리 파라미터에 담아 리다이렉트한다")
    void onAuthenticationFailure_oauth2Exception() throws Exception {
        OAuth2Error error = new OAuth2Error("ACCOUNT_LOCKED", "계정이 잠겼습니다.", null);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error, "계정이 잠겼습니다.");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).startsWith("https://mopl.io/sign-in");
        assertThat(redirectUrl).contains("error=ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("성공: 일반 인증 예외면 기본 에러 코드로 리다이렉트한다")
    void onAuthenticationFailure_genericException() throws Exception {
        BadCredentialsException exception = new BadCredentialsException("잘못된 인증 정보입니다.");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).contains("error=OAUTH2_LOGIN_FAILED");
    }
}
