package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMasterDataItemRequest(
        @NotBlank String code,
        @NotBlank String label,
        String description,
        Integer sortOrder,
        String metadata
) {}
