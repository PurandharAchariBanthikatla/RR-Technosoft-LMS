package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull ApplicationStatus status,
        String notes
) {}
