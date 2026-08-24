package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.NotificationSettings;
import com.rrtechnosoft.lms.entity.enums.DigestFrequency;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationSettingsResponse(
        UUID id,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        Boolean smtpConfigured,
        Boolean smtpUseTls,
        String fromName,
        String fromEmail,
        Boolean emailNotificationsEnabled,
        Boolean smsNotificationsEnabled,
        Boolean pushNotificationsEnabled,
        DigestFrequency digestFrequency,
        OffsetDateTime updatedAt
) {
    // Password is never returned to the client — only whether one is set.
    public static NotificationSettingsResponse from(NotificationSettings n) {
        return new NotificationSettingsResponse(n.getId(), n.getSmtpHost(), n.getSmtpPort(), n.getSmtpUsername(),
                n.getSmtpPasswordEncrypted() != null && !n.getSmtpPasswordEncrypted().isBlank(), n.getSmtpUseTls(),
                n.getFromName(), n.getFromEmail(), n.getEmailNotificationsEnabled(), n.getSmsNotificationsEnabled(),
                n.getPushNotificationsEnabled(), n.getDigestFrequency(), n.getUpdatedAt());
    }
}
