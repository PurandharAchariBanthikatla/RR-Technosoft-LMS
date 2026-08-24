package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for PATCH /users/me — a user editing their own name/phone. Not to
 * be confused with UpdateStudentRequest (an admin editing a student's
 * academic profile) — this is self-service and role-agnostic, so it never
 * touches studentId, role, status, or academic fields.
 */
public record UpdateProfileRequest(
        @NotBlank String fullName,
        String phone
) {}
