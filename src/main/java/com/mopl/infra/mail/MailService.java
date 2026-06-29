package com.mopl.infra.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendTempPassword(String toEmail, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[모두의 플리] 임시 비밀번호 안내");
        message.setText("임시 비밀번호: " + tempPassword + "\n\n3분 내에 로그인 후 비밀번호를 변경해주세요.");
        mailSender.send(message);
    }
}
