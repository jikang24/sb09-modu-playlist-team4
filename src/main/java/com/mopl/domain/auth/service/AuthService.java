package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.RefreshResult;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.port.in.AuthUseCase;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.global.auth.UserAuthInfo;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserAuthPort userAuthPort;
    private final PasswordResetWriteService passwordResetWriteService;
    private final AuthTokenService authTokenService;
    private final JwtProvider jwtProvider;

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        UserAuthInfo user = userAuthPort.findByEmail(request.email())
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        String tempPassword = generateTempPassword();
        passwordResetWriteService.resetPassword(user.id(), user.email(), tempPassword);
    }

    @Override
    public RefreshResult refresh(String refreshToken) {
        UUID userId = jwtProvider.parse(refreshToken).getUserId();
        ensureRefreshTokenValid(userId, refreshToken);

        UserAuthInfo user = userAuthPort.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.USER_NOT_FOUND));

        if (user.locked()) {
            throw new MoplException(ErrorCode.ACCOUNT_LOCKED);
        }

        String newAccessToken = jwtProvider.generateAccessToken(user.id(), user.email(), user.role().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.id(), user.email(), user.role().name());
        Duration refreshTtl = jwtProvider.calculateTtl(newRefreshToken);
        JwtClaims accessClaims = jwtProvider.parse(newAccessToken);

        try {
            authTokenService.saveRefreshToken(user.id(), newRefreshToken, refreshTtl);
            authTokenService.saveAccessJti(user.id(), accessClaims.getTokenId(), jwtProvider.getExpiration(newAccessToken));
        } catch (Exception e) {
            log.error("토큰 재발급 중 인증 저장소 오류 - userId: {}", userId, e);
            throw new MoplException(ErrorCode.AUTH_STORAGE_UNAVAILABLE);
        }

        log.info("토큰 재발급 완료 - userId: {}", userId);
        JwtDto jwtDto = new JwtDto(toUserDto(user), newAccessToken);
        return new RefreshResult(jwtDto, newRefreshToken, refreshTtl);
    }

    private void ensureRefreshTokenValid(UUID userId, String refreshToken) {
        try {
            if (!authTokenService.isValidRefreshToken(userId, refreshToken)) {
                throw new MoplException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
            }
        } catch (MoplException e) {
            throw e;
        } catch (Exception e) {
            log.error("리프레시 토큰 조회 중 인증 저장소 오류", e);
            throw new MoplException(ErrorCode.AUTH_STORAGE_UNAVAILABLE);
        }
    }

    private String generateTempPassword() {
        PasswordGenerator generator = new PasswordGenerator();
        return generator.generatePassword(12,
                new CharacterRule(EnglishCharacterData.UpperCase, 2),
                new CharacterRule(EnglishCharacterData.LowerCase, 2),
                new CharacterRule(EnglishCharacterData.Digit, 2),
                new CharacterRule(new CharacterData() {
                    @Override
                    public String getErrorCode() { return "INSUFFICIENT_SPECIAL"; }
                    @Override
                    public String getCharacters() { return "!@#$%&"; }
                }, 2));
    }

    private UserDto toUserDto(UserAuthInfo user) {
        return new UserDto(
                user.id(), user.createdAt(), user.email(),
                user.name(), user.profileImageUrl(), user.role(), user.locked());
    }
}
