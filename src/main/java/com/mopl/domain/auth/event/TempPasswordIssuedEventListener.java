package com.mopl.domain.auth.event;

import com.mopl.global.event.TempPasswordIssuedEvent;
import com.mopl.infra.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempPasswordIssuedEventListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TempPasswordIssuedEvent event) {
        mailService.sendTempPassword(event.email(), event.tempPassword());
    }
}