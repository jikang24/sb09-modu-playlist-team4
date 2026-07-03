package com.mopl.domain.auth.event;

import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.event.PasswordChangedEvent;
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

    //비밀번호 변경과 토큰 파기가 같은 트랜잭션 안에서 처리하기 위해 설정
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        passwordResetTokenPort.deleteByUserId(event.userId());
        log.info("임시 비밀번호 무효화 완료 - userId: {}", event.userId());
    }
}
