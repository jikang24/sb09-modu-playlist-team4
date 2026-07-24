package com.mopl.domain.auth.service;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.event.TempPasswordIssuedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// 임시 비밀번호 토큰 저장(DB 반영) + 이벤트 발행만 전담하는 트랜잭션 경계.
// S3 presigned URL 변환처럼 DB 커넥션을 붙잡을 필요 없는 작업(AuthService.resetPassword의 findByEmail)은
// 이 트랜잭션 밖에서 먼저 끝내고, 여기서는 순수 DB 쓰기와 커밋 후 이벤트 발행만 수행한다.
@Component
@RequiredArgsConstructor
class PasswordResetWriteService {

    private static final Duration TEMP_PASSWORD_TTL = Duration.ofMinutes(3);

    private final PasswordResetTokenPort passwordResetTokenPort;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    void resetPassword(UUID userId, String email, String tempPassword) {
        PasswordResetToken token = PasswordResetToken.create(
                userId,
                passwordEncoder.encode(tempPassword),
                Instant.now().plus(TEMP_PASSWORD_TTL));
        passwordResetTokenPort.replaceForUser(userId, token);

        eventPublisher.publishEvent(new TempPasswordIssuedEvent(userId, email, tempPassword));
    }
}
