package com.mopl.global.security;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.domain.user.dto.Role;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.security.userdetails.MoplUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MoplAuthenticationProvider 테스트")
class MoplAuthenticationProviderTest {

    @Mock
    private UserAuthPort userAuthPort;

    @Mock
    private PasswordResetTokenPort passwordResetTokenPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MoplAuthenticationProvider provider;

    private UUID userId;
    private UserAuthInfo user;

    @BeforeEach
    void setUp() {
        provider = new MoplAuthenticationProvider(userAuthPort, passwordResetTokenPort, passwordEncoder);
        userId = UUID.randomUUID();
        user = new UserAuthInfo(userId, Instant.now(), "woody@mopl.io", "encoded-password",
                "woody", null, Role.USER, false);
    }

    @Test
    @DisplayName("성공: 실제 비밀번호가 일치하면 인증에 성공한다")
    void authenticate_withRealPassword_success() {
        when(userAuthPort.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordResetTokenPort.findActiveByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("raw-password", user.password())).thenReturn(true);

        Authentication result = provider.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), "raw-password"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(MoplUserDetails.class);
        assertThat(((MoplUserDetails) result.getPrincipal()).getUserAuthInfo()).isEqualTo(user);
    }

    @Test
    @DisplayName("성공: 유효한 임시 비밀번호로 인증에 성공한다")
    void authenticate_withValidTemporaryPassword_success() {
        PasswordResetToken activeToken =
                PasswordResetToken.create(userId, "temp-encoded", Instant.now().plusSeconds(600));
        when(userAuthPort.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordResetTokenPort.findActiveByUserId(userId)).thenReturn(Optional.of(activeToken));
        when(passwordEncoder.matches("temp-raw", "temp-encoded")).thenReturn(true);

        Authentication result = provider.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), "temp-raw"));

        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 이메일이면 BadCredentialsException이 발생한다")
    void authenticate_userNotFound_throws() {
        when(userAuthPort.findByEmail("unknown@mopl.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken("unknown@mopl.io", "raw-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("실패: 잠긴 계정이면 LockedException이 발생한다")
    void authenticate_lockedUser_throws() {
        UserAuthInfo lockedUser = new UserAuthInfo(userId, Instant.now(), "woody@mopl.io", "encoded-password",
                "woody", null, Role.USER, true);
        when(userAuthPort.findByEmail(lockedUser.email())).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(lockedUser.email(), "raw-password")))
                .isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("실패: 비밀번호가 일치하지 않으면 BadCredentialsException이 발생한다")
    void authenticate_wrongPassword_throws() {
        when(userAuthPort.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordResetTokenPort.findActiveByUserId(userId)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong-password", user.password())).thenReturn(false);

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("실패: 임시 비밀번호가 존재하지만 일치하지 않으면 BadCredentialsException이 발생한다")
    void authenticate_wrongTemporaryPassword_throws() {
        PasswordResetToken activeToken =
                PasswordResetToken.create(userId, "temp-encoded", Instant.now().plusSeconds(600));
        when(userAuthPort.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordResetTokenPort.findActiveByUserId(userId)).thenReturn(Optional.of(activeToken));
        when(passwordEncoder.matches("wrong-temp", "temp-encoded")).thenReturn(false);

        assertThatThrownBy(() -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), "wrong-temp")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("성공: UsernamePasswordAuthenticationToken 타입을 지원한다")
    void supports_usernamePasswordAuthenticationToken() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }
}
