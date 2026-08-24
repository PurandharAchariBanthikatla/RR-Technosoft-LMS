package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateNotificationSettingsRequest;
import com.rrtechnosoft.lms.dto.response.NotificationSettingsResponse;
import com.rrtechnosoft.lms.entity.NotificationSettings;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final TextEncryptor textEncryptor;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public NotificationSettingsResponse get() {
        return NotificationSettingsResponse.from(loadSingleton());
    }

    @Transactional
    public NotificationSettingsResponse update(UpdateNotificationSettingsRequest request, UUID actorId) {
        NotificationSettings settings = loadSingleton();
        settings.setSmtpHost(request.smtpHost());
        if (request.smtpPort() != null) settings.setSmtpPort(request.smtpPort());
        settings.setSmtpUsername(request.smtpUsername());
        // Blank password means "keep the existing one" — the client never receives the real value back.
        if (request.smtpPassword() != null && !request.smtpPassword().isBlank()) {
            settings.setSmtpPasswordEncrypted(textEncryptor.encrypt(request.smtpPassword()));
        }
        settings.setSmtpUseTls(request.smtpUseTls());
        settings.setFromName(request.fromName());
        settings.setFromEmail(request.fromEmail());
        settings.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        settings.setSmsNotificationsEnabled(request.smsNotificationsEnabled());
        settings.setPushNotificationsEnabled(request.pushNotificationsEnabled());
        settings.setDigestFrequency(request.digestFrequency());
        settings.setUpdatedBy(actorId);
        notificationSettingsRepository.save(settings);
        auditLogService.log(actorId, "UPDATE_NOTIFICATION_SETTINGS", "NotificationSettings", settings.getId(), null);
        return NotificationSettingsResponse.from(settings);
    }

    private NotificationSettings loadSingleton() {
        return notificationSettingsRepository.findBySingletonGuardTrue()
                .orElseThrow(() -> ApiException.notFound("Notification settings not configured"));
    }
}
