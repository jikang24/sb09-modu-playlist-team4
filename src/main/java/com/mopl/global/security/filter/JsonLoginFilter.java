package com.mopl.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.Map;

public class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonLoginFilter(AuthenticationManager authenticationManager) {
        super("/api/auth/sign-in");
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String email = null;
        String password = null;

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                Map<String, String> body = objectMapper.readValue(request.getInputStream(), Map.class);
                email = body.get("email");
                password = body.get("password");
            } catch (IOException e) {
                throw new AuthenticationServiceException("Failed to parse JSON body", e);
            }
        } else {
            email = request.getParameter("username");
            password = request.getParameter("password");
        }

        if (email == null || password == null) {
            throw new AuthenticationServiceException("username or password is missing");
        }

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, password);
        return getAuthenticationManager().authenticate(token);
    }
}