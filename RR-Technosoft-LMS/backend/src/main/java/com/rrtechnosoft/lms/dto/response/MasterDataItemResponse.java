package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.MasterDataItem;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MasterDataItemResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String code,
        String label,
        String description,
        Integer sortOrder,
        Boolean isActive,
        String metadata,
        OffsetDateTime updatedAt
) {
    public static MasterDataItemResponse from(MasterDataItem i) {
        return new MasterDataItemResponse(i.getId(), i.getCategory().getId(), i.getCategory().getCode(),
                i.getCode(), i.getLabel(), i.getDescription(), i.getSortOrder(), i.getIsActive(),
                i.getMetadata(), i.getUpdatedAt());
    }
}
