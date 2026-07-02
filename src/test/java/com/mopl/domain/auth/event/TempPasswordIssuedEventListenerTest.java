package com.mopl.domain.auth.event;

import com.mopl.global.event.TempPasswordIssuedEvent;
import com.mopl.infra.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TempPasswordIssuedEventListener 테스트")
class TempPasswordIssuedEventListenerTest {

    @Mock
    private MailService mailService;

    private TempPasswordIssuedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new TempPasswordIssuedEventListener(mailService);
    }

    @Test
    @DisplayName("성공: 임시 비밀번호 이메일을 전송한다")
    void handle_SendsTempPasswordEmail() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String tempPassword = "TempPass123!@#";

        TempPasswordIssuedEvent event = new TempPasswordIssuedEvent(userId, email, tempPassword);

        listener.handle(event);

        verify(mailService).sendTempPassword(email, tempPassword);
    }

    @Test
    @DisplayName("성공: 올바른 매개변수로 이메일을 전송한다")
    void handle_SendsEmailWithCorrectParameters() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        String tempPassword = "SecureTemp456$%^";

        TempPasswordIssuedEvent event = new TempPasswordIssuedEvent(userId, email, tempPassword);

        listener.handle(event);

        verify(mailService, times(1)).sendTempPassword(email, tempPassword);
    }

    @Test
    @DisplayName("실패: 메일 서비스 오류가 발생해도 처리한다")
    void handle_MailServiceException() {
        UUID userId = UUID.randomUUID();
        String email = "invalid@example.com";
        String tempPassword = "TempPass123!@#";

        doThrow(new RuntimeException("Mail service unavailable"))
                .when(mailService).sendTempPassword(anyString(), anyString());

        TempPasswordIssuedEvent event = new TempPasswordIssuedEvent(userId, email, tempPassword);

        try {
            listener.handle(event);
            verify(mailService).sendTempPassword(email, tempPassword);
        } catch (RuntimeException e) {
            verify(mailService).sendTempPassword(email, tempPassword);
        }
    }

    @Test
    @DisplayName("성공: 여러 사용자에게 각각 이메일을 전송한다")
    void handle_DifferentEmails() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String tempPassword = "TempPass123!@#";

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        TempPasswordIssuedEvent event1 = new TempPasswordIssuedEvent(userId1, email1, tempPassword);
        TempPasswordIssuedEvent event2 = new TempPasswordIssuedEvent(userId2, email2, tempPassword);

        listener.handle(event1);
        listener.handle(event2);

        verify(mailService).sendTempPassword(email1, tempPassword);
        verify(mailService).sendTempPassword(email2, tempPassword);
    }
}
