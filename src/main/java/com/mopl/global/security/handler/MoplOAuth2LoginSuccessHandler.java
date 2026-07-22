package com.mopl.global.security.handler;

import com.mopl.domain.auth.adapter.in.oauth2.UserIdAware;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenIssuer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class MoplOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthTokenIssuer authTokenIssuer;
    private final UserAuthPort userAuthPort;

    @Value("${app.frontend.base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserIdAware principal = (UserIdAware) authentication.getPrincipal();
        UserAuthInfo user = userAuthPort.findById(principal.getUserId())
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        try {
            authTokenIssuer.issue(user, response);
        } catch (MoplException e) {
            log.error("소셜 로그인 처리 중 토큰 발급 실패 - userId: {}", user.id(), e);
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                    .path("/sign-in")
                    .queryParam("error", e.getErrorCode().name())
                    .queryParam("error_message", e.getErrorCode().getMessage())
                    .encode()
                    .build()
                    .toUriString();
            response.sendRedirect(errorRedirectUrl);
            return;
        }
        log.info("소셜 로그인 성공 - userId: {}", user.id());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/")
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
