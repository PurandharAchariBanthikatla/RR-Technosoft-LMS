package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.SecuritySettings;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SecuritySettingsResponse(
        UUID id,
        Integer passwordMinLength,
        Boolean passwordRequireUppercase,
        Boolean passwordRequireNumber,
        Boolean passwordRequireSpecialChar,
        Integer passwordExpiryDays,
        Integer maxLoginAttempts,
        Integer lockoutDurationMinutes,
        Integer sessionTimeoutMinutes,
        Boolean mfaRequiredForAdmins,
        String allowedIpRanges,
        Boolean forceLogoutOnPasswordChange,
        OffsetDateTime updatedAt
) {
    public static SecuritySettingsResponse from(SecuritySettings s) {
        return new SecuritySettingsResponse(s.getId(), s.getPasswordMinLength(), s.getPasswordRequireUppercase(),
                s.getPasswordRequireNumber(), s.getPasswordRequireSpecialChar(), s.getPasswordExpiryDays(),
                s.getMaxLoginAttempts(), s.getLockoutDurationMinutes(), s.getSessionTimeoutMinutes(),
                s.getMfaRequiredForAdmins(), s.getAllowedIpRanges(), s.getForceLogoutOnPasswordChange(),
                s.getUpdatedAt());
    }
}
