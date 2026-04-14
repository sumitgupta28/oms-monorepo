package com.oms.notification.sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service @Profile("prod") @RequiredArgsConstructor @Slf4j
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to); msg.setSubject(subject); msg.setText(body);
        mailSender.send(msg);
        log.info("Email sent to {}", to);
    }
}
