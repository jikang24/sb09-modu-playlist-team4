package com.mopl.domain.auth.port.in;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;

public interface AuthUseCase {
    void resetPassword(ResetPasswordRequest request);

    JwtDto refresh(String refreshToken);
}
