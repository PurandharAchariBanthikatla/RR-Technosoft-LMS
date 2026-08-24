package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEnrollmentStatusRequest(
        @NotNull EnrollmentStatus status
) {}
