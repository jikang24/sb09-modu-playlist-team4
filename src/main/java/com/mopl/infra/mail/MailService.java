package com.mopl.infra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
    public class MailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTempPassword(String toEmail, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[모두의 플리] 임시 비밀번호 안내");
            message.setText("임시 비밀번호: " + tempPassword +
                    "\n\n3분 내에 로그인 후 비밀번호를 변경해주세요." +
                    "\n\n발급 시각: " + java.time.Instant.now(java.time.ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[메일 발송 실패] to={}, error={}", toEmail, e.getMessage(), e);
        }
    }
}
