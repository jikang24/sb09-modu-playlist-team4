package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.dto.SignInRequest;
import com.mopl.domain.auth.repository.PasswordResetTokenRepository;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration TEMP_PASSWORD_TTL = Duration.ofMinutes(3);

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public JwtDto signIn(SignInRequest request) {
        return null;
    }

    @Override
    public void signOut() {

    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {

    }

    @Override
    public JwtDto refresh(String refreshToken) {
        return null;
    }

    @Override
    public void csrf() {

    }
}
