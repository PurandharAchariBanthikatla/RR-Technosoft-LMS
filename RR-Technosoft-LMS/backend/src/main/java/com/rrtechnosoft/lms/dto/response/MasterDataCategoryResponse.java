package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.MasterDataCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MasterDataCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        Boolean isSystem,
        OffsetDateTime updatedAt
) {
    public static MasterDataCategoryResponse from(MasterDataCategory c) {
        return new MasterDataCategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                c.getIsSystem(), c.getUpdatedAt());
    }
}
