package com.mopl.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class MoplOAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String DEFAULT_ERROR_CODE = "OAUTH2_LOGIN_FAILED";

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String errorCode = DEFAULT_ERROR_CODE;
        if (exception instanceof OAuth2AuthenticationException oAuth2Exception
                && oAuth2Exception.getError().getErrorCode() != null) {
            errorCode = oAuth2Exception.getError().getErrorCode();
        }
        log.warn("소셜 로그인 실패 - {}", exception.getMessage());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/sign-in")
                .queryParam("error", errorCode)
                .queryParam("error_message", exception.getMessage())
                .encode()
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
