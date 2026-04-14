package com.oms.notification.sender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
@Service @Profile("local") @Slf4j
public class ConsoleEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        log.info("EMAIL [local]\n  To: {}\n  Subject: {}\n  Body: {}", to, subject, body);
    }
}
