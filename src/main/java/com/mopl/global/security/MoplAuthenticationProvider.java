package com.mopl.global.security;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.security.userdetails.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

// 임시 비밀번호가 유효한 경우 임시 비밀번호로만 인증, 아닌 경우 실제 비밀번호로 인증
@RequiredArgsConstructor
public class MoplAuthenticationProvider implements AuthenticationProvider {

    private final UserAuthPort userAuthPort;
    private final PasswordResetTokenPort passwordResetTokenPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        UserAuthInfo user = userAuthPort.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.locked()) {
            throw new LockedException("잠긴 계정입니다.");
        }

        PasswordResetToken activeToken = passwordResetTokenPort
                .findActiveByUserId(user.id())
                .filter(PasswordResetToken::isValid)
                .orElse(null);

        if (activeToken != null) {
            if (!passwordEncoder.matches(rawPassword, activeToken.getTemporaryPassword())) {
                throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }
        } else {
            if (!passwordEncoder.matches(rawPassword, user.password())) {
                throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }
        }

        MoplUserDetails userDetails = new MoplUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}