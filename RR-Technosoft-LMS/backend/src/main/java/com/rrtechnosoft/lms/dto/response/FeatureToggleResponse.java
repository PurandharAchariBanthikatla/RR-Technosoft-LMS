package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.FeatureToggle;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FeatureToggleResponse(
        UUID id,
        String featureKey,
        String name,
        String description,
        Boolean enabled,
        OffsetDateTime updatedAt
) {
    public static FeatureToggleResponse from(FeatureToggle f) {
        return new FeatureToggleResponse(f.getId(), f.getFeatureKey(), f.getName(), f.getDescription(),
                f.getEnabled(), f.getUpdatedAt());
    }
}
