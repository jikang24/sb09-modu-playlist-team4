package com.mopl.domain.auth.service;

import com.mopl.domain.auth.domain.PasswordResetToken;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.event.TempPasswordIssuedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetWriteService 테스트")
class PasswordResetWriteServiceTest {

    @Mock
    private PasswordResetTokenPort passwordResetTokenPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PasswordResetWriteService passwordResetWriteService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        passwordResetWriteService = new PasswordResetWriteService(
                passwordResetTokenPort,
                passwordEncoder,
                eventPublisher
        );
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 토큰을 저장하고 이벤트를 발행한다")
    void resetPassword() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_temp_password");

        passwordResetWriteService.resetPassword(testUserId, "test@email.com", "tempPassword123!");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenPort).replaceForUser(eq(testUserId), tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<TempPasswordIssuedEvent> eventCaptor = ArgumentCaptor.forClass(TempPasswordIssuedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TempPasswordIssuedEvent event = eventCaptor.getValue();
        assertEquals(testUserId, event.userId());
        assertEquals("test@email.com", event.email());
        assertEquals("tempPassword123!", event.tempPassword());
    }
}
