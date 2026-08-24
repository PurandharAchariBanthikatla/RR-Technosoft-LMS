package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMasterDataItemRequest(
        @NotBlank String label,
        String description,
        Integer sortOrder,
        Boolean isActive,
        String metadata
) {}
