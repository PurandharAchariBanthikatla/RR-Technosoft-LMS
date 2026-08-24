package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSecuritySettingsRequest(
        @Min(6) @NotNull Integer passwordMinLength,
        @NotNull Boolean passwordRequireUppercase,
        @NotNull Boolean passwordRequireNumber,
        @NotNull Boolean passwordRequireSpecialChar,
        @Min(0) @NotNull Integer passwordExpiryDays,
        @Min(1) @NotNull Integer maxLoginAttempts,
        @Min(1) @NotNull Integer lockoutDurationMinutes,
        @Min(1) @NotNull Integer sessionTimeoutMinutes,
        @NotNull Boolean mfaRequiredForAdmins,
        String allowedIpRanges,
        @NotNull Boolean forceLogoutOnPasswordChange
) {}
