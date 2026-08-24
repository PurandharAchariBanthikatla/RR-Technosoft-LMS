package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull UUID courseId
) {}
