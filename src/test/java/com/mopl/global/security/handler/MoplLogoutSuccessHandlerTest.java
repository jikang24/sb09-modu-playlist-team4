package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoplLogoutSuccessHandler 테스트")
class MoplLogoutSuccessHandlerTest {

    private MoplLogoutSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplLogoutSuccessHandler(new ObjectMapper());
    }

    @Test
    @DisplayName("성공: 인증 정보가 없어도 204 상태와 공통 성공 응답을 반환한다")
    void onLogoutSuccess_withoutAuthentication_writesNoContentResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(request, response, null);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("\"status\":200");
    }

    @Test
    @DisplayName("성공: 인증 정보가 있어도 동일하게 204 상태를 반환한다")
    void onLogoutSuccess_withAuthentication_stillReturnsNoContent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = new UsernamePasswordAuthenticationToken("woody@mopl.io", null);

        handler.onLogoutSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(204);
    }
}
