package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoplAuthenticationEntryPoint 테스트")
class MoplAuthenticationEntryPointTest {

    private MoplAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        entryPoint = new MoplAuthenticationEntryPoint(new ObjectMapper());
    }

    @Test
    @DisplayName("성공: 인증되지 않은 요청은 401과 UNAUTHORIZED 에러 응답을 반환한다")
    void commence_badCredentials_writesUnauthorizedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("잘못된 인증 정보입니다."));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED").contains("인증이 필요합니다.");
    }

    @Test
    @DisplayName("실패: 인증 정보가 아예 없는 예외에서도 동일한 형식으로 401 응답을 반환한다")
    void commence_insufficientAuthentication_stillReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("인증 정보 부족"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}
