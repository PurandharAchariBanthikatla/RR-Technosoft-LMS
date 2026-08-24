package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.SystemSetting;
import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import com.rrtechnosoft.lms.entity.enums.SettingValueType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SystemSettingResponse(
        UUID id,
        String key,
        String value,
        SettingValueType valueType,
        SettingCategory category,
        String description,
        Boolean isEditable,
        OffsetDateTime updatedAt
) {
    public static SystemSettingResponse from(SystemSetting s) {
        return new SystemSettingResponse(s.getId(), s.getSettingKey(), s.getSettingValue(), s.getValueType(),
                s.getCategory(), s.getDescription(), s.getIsEditable(), s.getUpdatedAt());
    }
}
