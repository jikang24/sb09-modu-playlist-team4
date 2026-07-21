package com.mopl.domain.auth.port.in;

import com.mopl.domain.auth.dto.RefreshResult;
import com.mopl.domain.auth.dto.ResetPasswordRequest;

public interface AuthUseCase {
    void resetPassword(ResetPasswordRequest request);

    RefreshResult refresh(String refreshToken);
}
