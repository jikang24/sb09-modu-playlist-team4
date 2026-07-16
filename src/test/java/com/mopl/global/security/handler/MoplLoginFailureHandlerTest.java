package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoplLoginFailureHandler 테스트")
class MoplLoginFailureHandlerTest {

    private MoplLoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplLoginFailureHandler(new ObjectMapper());
    }

    @Test
    @DisplayName("실패: 잠긴 계정이면 403과 USER_LOCKED 코드를 반환한다")
    void onAuthenticationFailure_lockedException_returnsForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new LockedException("잠긴 계정입니다."));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("USER_LOCKED");
    }

    @Test
    @DisplayName("실패: 비밀번호가 틀리면 401과 INVALID_CREDENTIALS 코드를 반환한다")
    void onAuthenticationFailure_badCredentials_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("잘못된 인증 정보입니다."));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("실패: 그 외 인증 예외는 401과 기본 UNAUTHORIZED 코드를 반환한다")
    void onAuthenticationFailure_genericException_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response,
                new AuthenticationServiceException("인증 서비스 오류"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}
