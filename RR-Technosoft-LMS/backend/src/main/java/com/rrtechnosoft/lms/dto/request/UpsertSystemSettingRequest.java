package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import com.rrtechnosoft.lms.entity.enums.SettingValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertSystemSettingRequest(
        @NotBlank String key,
        String value,
        @NotNull SettingValueType valueType,
        @NotNull SettingCategory category,
        String description
) {}
