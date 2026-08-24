package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMasterDataCategoryRequest(
        @NotBlank String name,
        String description
) {}
