package com.mopl.domain.user.event;

import com.mopl.global.jwt.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserLockedEventListener {

    private final AuthTokenService authTokenService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLocked(UserLockedEvent event) {
        authTokenService.forceLogoutByUserId(event.userId());
    }
}
