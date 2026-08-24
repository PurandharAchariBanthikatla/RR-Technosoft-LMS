package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateMasterDataCategoryRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]+$", message = "code must be upper-case letters, digits and underscores")
        String code,
        @NotBlank String name,
        String description
) {}
