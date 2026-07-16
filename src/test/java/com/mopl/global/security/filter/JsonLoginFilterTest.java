package com.mopl.global.security.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonLoginFilter 테스트")
class JsonLoginFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authenticationResult;

    private JsonLoginFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JsonLoginFilter(authenticationManager);
    }

    @Test
    @DisplayName("성공: JSON 요청 본문의 email, password로 인증을 시도한다")
    void attemptAuthentication_jsonBody_success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"email\":\"woody@mopl.io\",\"password\":\"1234\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);

        Authentication result = filter.attemptAuthentication(request, response);

        assertThat(result).isEqualTo(authenticationResult);
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("woody@mopl.io", "1234"));
    }

    @Test
    @DisplayName("성공: JSON이 아닌 요청은 파라미터의 username, password로 인증을 시도한다")
    void attemptAuthentication_formParams_success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "woody@mopl.io");
        request.setParameter("password", "1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any())).thenReturn(authenticationResult);

        Authentication result = filter.attemptAuthentication(request, response);

        assertThat(result).isEqualTo(authenticationResult);
    }

    @Test
    @DisplayName("실패: JSON 본문에 email 또는 password가 없으면 예외가 발생한다")
    void attemptAuthentication_missingJsonFields_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"email\":\"woody@mopl.io\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(request, response))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    @DisplayName("실패: form 파라미터에 username 또는 password가 없으면 예외가 발생한다")
    void attemptAuthentication_missingFormFields_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "woody@mopl.io");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(request, response))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    @DisplayName("실패: JSON 파싱에 실패하면 예외가 발생한다")
    void attemptAuthentication_invalidJson_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("not-a-json".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(request, response))
                .isInstanceOf(AuthenticationServiceException.class);
    }
}
