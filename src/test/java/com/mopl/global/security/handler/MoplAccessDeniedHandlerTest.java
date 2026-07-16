package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoplAccessDeniedHandler 테스트")
class MoplAccessDeniedHandlerTest {

    private MoplAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoplAccessDeniedHandler(new ObjectMapper());
    }

    @Test
    @DisplayName("성공: 접근 권한이 없으면 403과 FORBIDDEN 에러 응답을 반환한다")
    void handle_writesForbiddenResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("접근 거부"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN").contains("권한이 없습니다.");
    }

    @Test
    @DisplayName("실패: 상세 메시지가 없는 예외에서도 동일한 형식으로 403 응답을 반환한다")
    void handle_withNoMessageException_stillReturnsForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException(null));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
    }
}
