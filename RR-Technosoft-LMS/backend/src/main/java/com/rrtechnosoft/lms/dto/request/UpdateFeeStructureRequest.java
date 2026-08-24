package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateFeeStructureRequest(
        @Size(min = 3, max = 150) String name,
        String description,
        Boolean isActive
) {}
