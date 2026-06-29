package com.mopl.global.security;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.security.userdetails.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

// 임시 비밀번호가 유효한 경우 임시 비밀번호로만 인증, 아닌 경우 실제 비밀번호로 인증
@RequiredArgsConstructor
public class MoplAuthenticationProvider extends DaoAuthenticationProvider {

    private final PasswordResetTokenPort passwordResetTokenPort;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("자격증명이 제공되지 않았습니다.");
        }

        MoplUserDetails moplUser = (MoplUserDetails) userDetails;

        if (moplUser.getUserAuthInfo().locked()) {
            throw new LockedException("잠긴 계정입니다.");
        }

        String rawPassword = authentication.getCredentials().toString();
        PasswordEncoder encoder = getPasswordEncoder();

        PasswordResetToken activeToken = passwordResetTokenPort
                .findActiveByUserId(moplUser.getUserAuthInfo().id())
                .filter(PasswordResetToken::isValid)
                .orElse(null);

        if (activeToken != null) {
            if (!encoder.matches(rawPassword, activeToken.getTemporaryPassword())) {
                throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }
            return;
        }

        if (!encoder.matches(rawPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }
}
