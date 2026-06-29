package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class MoplLoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        int status;
        String code;
        String message;

        if (exception instanceof LockedException) {
            status = HttpStatus.FORBIDDEN.value();
            code = "USER_LOCKED";
            message = "잠긴 계정입니다.";
        } else if (exception instanceof BadCredentialsException) {
            status = HttpStatus.UNAUTHORIZED.value();
            code = "INVALID_CREDENTIALS";
            message = "이메일 또는 비밀번호가 올바르지 않습니다.";
        } else {
            status = HttpStatus.UNAUTHORIZED.value();
            code = "UNAUTHORIZED";
            message = "인증에 실패했습니다.";
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
