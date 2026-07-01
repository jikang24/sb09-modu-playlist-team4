package com.mopl.domain.auth.event;

import com.mopl.global.event.TempPasswordIssuedEvent;
import com.mopl.infra.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempPasswordIssuedEventListener {

    private final MailService mailService;

    @Async
    @EventListener
    public void handle(TempPasswordIssuedEvent event) {
        mailService.sendTempPassword(event.email(), event.tempPassword());
    }
}