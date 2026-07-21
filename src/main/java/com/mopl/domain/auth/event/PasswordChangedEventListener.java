package com.mopl.domain.auth.event;

import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.event.PasswordChangedEvent;
import com.mopl.global.jwt.AuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangedEventListener {
    private final PasswordResetTokenPort passwordResetTokenPort;
    private final AuthTokenService authTokenService;

    //비밀번호 변경과 토큰 파기가 같은 트랜잭션 안에서 처리하기 위해 설정
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        passwordResetTokenPort.deleteByUserId(event.userId());
        log.info("임시 비밀번호 무효화 완료 - userId: {}", event.userId());
    }

    //비밀번호 변경 시 탈취된 세션이 만료 전까지 계속 유효한 것을 방지하기 위해 강제 로그아웃 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChangedForceLogout(PasswordChangedEvent event) {
        authTokenService.forceLogoutByUserId(event.userId());
    }
}
