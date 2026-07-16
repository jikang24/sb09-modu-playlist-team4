package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.jwt.AuthTokenIssuer;
import com.mopl.global.security.userdetails.MoplUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoplLoginSuccessHandler 테스트")
class MoplLoginSuccessHandlerTest {

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    private MoplLoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new MoplLoginSuccessHandler(authTokenIssuer, objectMapper);
    }

    @Test
    @DisplayName("성공: 토큰을 발급하고 사용자 정보와 액세스 토큰을 응답한다")
    void onAuthenticationSuccess_writesUserAndToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAuthInfo user = new UserAuthInfo(userId, Instant.now(), "woody@mopl.io", "encoded",
                "woody", null, Role.USER, false);
        MoplUserDetails userDetails = new MoplUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(authTokenIssuer.issue(eq(user), any())).thenReturn("access-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString())
                .contains("access-token")
                .contains("woody@mopl.io");
    }

    @Test
    @DisplayName("실패: principal이 MoplUserDetails가 아니면 ClassCastException이 발생한다")
    void onAuthenticationSuccess_wrongPrincipalType_throws() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("woody@mopl.io", null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(ClassCastException.class);
    }
}
