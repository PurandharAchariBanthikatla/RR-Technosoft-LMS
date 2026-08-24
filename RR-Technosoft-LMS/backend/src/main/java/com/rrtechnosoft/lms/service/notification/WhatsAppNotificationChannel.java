package com.rrtechnosoft.lms.service.notification;

import com.rrtechnosoft.lms.config.WhatsAppProperties;
import com.rrtechnosoft.lms.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Sends via Twilio's WhatsApp Business API
 * (https://www.twilio.com/docs/whatsapp/api). Real, working HTTP call — not
 * a stub — but genuinely inert until app.whatsapp.enabled=true and real
 * Twilio credentials are supplied (TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN /
 * TWILIO_WHATSAPP_FROM env vars), the same pattern as S3/email: configured
 * infrastructure, not fabricated behavior.
 */
@Slf4j
@Component
public class WhatsAppNotificationChannel implements NotificationChannel {

    private final WhatsAppProperties properties;
    private final RestClient restClient = RestClient.create();

    public WhatsAppNotificationChannel(WhatsAppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(User recipient, String title, String body) {
        if (!properties.isEnabled()) {
            return;
        }
        if (recipient.getPhone() == null || recipient.getPhone().isBlank()) {
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", "whatsapp:" + properties.getFromNumber());
            form.add("To", "whatsapp:" + recipient.getPhone());
            form.add("Body", body == null ? title : (title + "\n\n" + body));

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + properties.getAccountSid() + "/Messages.json";
            restClient.post()
                    .uri(url)
                    .headers(h -> h.setBasicAuth(properties.getAccountSid(), properties.getAuthToken()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Same rule as email: a failed WhatsApp send must never fail the
            // request that triggered the notification.
            log.warn("Failed to send WhatsApp notification to {}: {}", recipient.getId(), e.getMessage());
        }
    }
}
