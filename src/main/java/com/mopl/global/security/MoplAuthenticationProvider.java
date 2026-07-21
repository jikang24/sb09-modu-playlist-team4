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

        PasswordResetToken activeToken = passwordResetTokenPort
                .findActiveByUserId(user.id())
                .filter(PasswordResetToken::isValid)
                .orElse(null);

        // 비밀번호가 틀린 경우 계정 존재 여부/잠금 여부와 무관하게 항상 동일한 예외(401)를 반환하기 위해 비밀번호 검증을 잠금 여부 체크보다 먼저 수행
        boolean passwordMatches = activeToken != null
                ? passwordEncoder.matches(rawPassword, activeToken.getTemporaryPassword())
                : passwordEncoder.matches(rawPassword, user.password());

        if (!passwordMatches) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (user.locked()) {
            throw new LockedException("잠긴 계정입니다.");
        }

        if (activeToken != null) {
            activeToken.markUsed();
            passwordResetTokenPort.save(activeToken);
        }

        MoplUserDetails userDetails = new MoplUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}