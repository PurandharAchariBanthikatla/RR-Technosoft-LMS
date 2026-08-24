package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.DigestFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequest(
        String smtpHost,
        @Min(1) @Max(65535) Integer smtpPort,
        String smtpUsername,
        String smtpPassword,          // optional — left blank keeps the existing stored value
        @NotNull Boolean smtpUseTls,
        @NotBlank String fromName,
        @Email String fromEmail,
        @NotNull Boolean emailNotificationsEnabled,
        @NotNull Boolean smsNotificationsEnabled,
        @NotNull Boolean pushNotificationsEnabled,
        @NotNull DigestFrequency digestFrequency
) {}
