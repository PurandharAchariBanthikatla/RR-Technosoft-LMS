package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateSecuritySettingsRequest;
import com.rrtechnosoft.lms.dto.response.SecuritySettingsResponse;
import com.rrtechnosoft.lms.entity.SecuritySettings;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.SecuritySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Also consumed at runtime by {@link AuthService} (lockout policy) so
 * changes made here take effect immediately, without a redeploy.
 */
@Service
@RequiredArgsConstructor
public class SecuritySettingsService {

    private final SecuritySettingsRepository securitySettingsRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public SecuritySettingsResponse get() {
        return SecuritySettingsResponse.from(loadSingleton());
    }

    @Transactional(readOnly = true)
    public SecuritySettings getEntity() {
        return loadSingleton();
    }

    @Transactional
    public SecuritySettingsResponse update(UpdateSecuritySettingsRequest request, UUID actorId) {
        SecuritySettings settings = loadSingleton();
        settings.setPasswordMinLength(request.passwordMinLength());
        settings.setPasswordRequireUppercase(request.passwordRequireUppercase());
        settings.setPasswordRequireNumber(request.passwordRequireNumber());
        settings.setPasswordRequireSpecialChar(request.passwordRequireSpecialChar());
        settings.setPasswordExpiryDays(request.passwordExpiryDays());
        settings.setMaxLoginAttempts(request.maxLoginAttempts());
        settings.setLockoutDurationMinutes(request.lockoutDurationMinutes());
        settings.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        settings.setMfaRequiredForAdmins(request.mfaRequiredForAdmins());
        settings.setAllowedIpRanges(request.allowedIpRanges());
        settings.setForceLogoutOnPasswordChange(request.forceLogoutOnPasswordChange());
        settings.setUpdatedBy(actorId);
        securitySettingsRepository.save(settings);
        auditLogService.log(actorId, "UPDATE_SECURITY_SETTINGS", "SecuritySettings", settings.getId(), null);
        return SecuritySettingsResponse.from(settings);
    }

    private SecuritySettings loadSingleton() {
        return securitySettingsRepository.findBySingletonGuardTrue()
                .orElseThrow(() -> ApiException.notFound("Security settings not configured"));
    }
}
