package com.mopl.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.exception.ErrorResponse;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenIssuer;
import com.mopl.global.security.userdetails.MoplUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class MoplLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthTokenIssuer authTokenIssuer;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        MoplUserDetails userDetails = (MoplUserDetails) authentication.getPrincipal();
        UserAuthInfo user = userDetails.getUserAuthInfo();

        String accessToken;
        try {
            accessToken = authTokenIssuer.issue(user, response);
        } catch (MoplException e) {
            log.error("로그인 처리 중 토큰 발급 실패 - userId: {}", user.id(), e);
            response.setStatus(e.getErrorCode().getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse(e.getErrorCode().name(), e.getErrorCode().getMessage()));
            return;
        }
        log.info("로그인 성공 - userId: {}", user.id());

        UserDto userDto = new UserDto(
                user.id(), user.createdAt(), user.email(),
                user.name(), user.profileImageUrl(), user.role(), user.locked()
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new JwtDto(userDto, accessToken));
    }
}
