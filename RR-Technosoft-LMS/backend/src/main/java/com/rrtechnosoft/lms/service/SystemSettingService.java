package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateSystemSettingValueRequest;
import com.rrtechnosoft.lms.dto.request.UpsertSystemSettingRequest;
import com.rrtechnosoft.lms.dto.response.SystemSettingResponse;
import com.rrtechnosoft.lms.entity.SystemSetting;
import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> list(SettingCategory category) {
        List<SystemSetting> settings = category != null
                ? systemSettingRepository.findByCategoryOrderBySettingKeyAsc(category)
                : systemSettingRepository.findAllByOrderByCategoryAscSettingKeyAsc();
        return settings.stream().map(SystemSettingResponse::from).toList();
    }

    @Transactional
    public SystemSettingResponse create(UpsertSystemSettingRequest request, UUID actorId) {
        if (systemSettingRepository.existsBySettingKey(request.key())) {
            throw ApiException.conflict("A setting with key '" + request.key() + "' already exists");
        }
        SystemSetting setting = SystemSetting.builder()
                .settingKey(request.key())
                .settingValue(request.value())
                .valueType(request.valueType())
                .category(request.category())
                .description(request.description())
                .isEditable(true)
                .updatedBy(actorId)
                .build();
        setting = systemSettingRepository.save(setting);
        auditLogService.log(actorId, "CREATE_SYSTEM_SETTING", "SystemSetting", setting.getId(), null);
        return SystemSettingResponse.from(setting);
    }

    @Transactional
    public SystemSettingResponse updateValue(UUID id, UpdateSystemSettingValueRequest request, UUID actorId) {
        SystemSetting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Setting not found"));
        if (!Boolean.TRUE.equals(setting.getIsEditable())) {
            throw ApiException.badRequest("This setting is not editable");
        }
        setting.setSettingValue(request.value());
        setting.setUpdatedBy(actorId);
        systemSettingRepository.save(setting);
        auditLogService.log(actorId, "UPDATE_SYSTEM_SETTING", "SystemSetting", setting.getId(), null);
        return SystemSettingResponse.from(setting);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        SystemSetting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Setting not found"));
        if (!Boolean.TRUE.equals(setting.getIsEditable())) {
            throw ApiException.badRequest("This setting cannot be deleted");
        }
        systemSettingRepository.delete(setting);
        auditLogService.log(actorId, "DELETE_SYSTEM_SETTING", "SystemSetting", id, null);
    }
}
