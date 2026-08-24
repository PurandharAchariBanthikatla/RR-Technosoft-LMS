package com.rrtechnosoft.lms.service.notification;

import com.rrtechnosoft.lms.config.AppProperties;
import com.rrtechnosoft.lms.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Override
    public void send(User recipient, String title, String body) {
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return; // students without a stored email can't receive this channel
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMailFromAddress());
            message.setTo(recipient.getEmail());
            message.setSubject(title);
            message.setText(body == null ? title : body);
            mailSender.send(message);
        } catch (Exception e) {
            // A failed email must never fail the request that triggered the
            // notification (e.g. grading an assignment) — log and move on.
            log.warn("Failed to send notification email to {}: {}", recipient.getId(), e.getMessage());
        }
    }
}
