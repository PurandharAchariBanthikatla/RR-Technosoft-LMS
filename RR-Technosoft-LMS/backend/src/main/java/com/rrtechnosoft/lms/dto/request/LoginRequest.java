package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * identifier = email for SUPER_ADMIN/ADMIN, or Student ID (e.g. RRT2026S0001) for STUDENT.
 * The auth service figures out which lookup to use based on format.
 */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {}
