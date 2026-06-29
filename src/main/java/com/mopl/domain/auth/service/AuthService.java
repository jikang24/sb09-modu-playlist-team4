package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.dto.SignInRequest;

public interface AuthService {
    JwtDto signIn(SignInRequest request);

    void signOut();

    void resetPassword(ResetPasswordRequest request);

    JwtDto refresh(String refreshToken);

    void csrf();
}
