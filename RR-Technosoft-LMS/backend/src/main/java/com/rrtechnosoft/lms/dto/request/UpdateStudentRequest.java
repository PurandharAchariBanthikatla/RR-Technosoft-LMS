package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for PUT /students/manage/{id} — editing an existing student's
 * profile details. Deliberately excludes studentId (server-generated,
 * immutable), initialPassword (use a separate reset-password flow), and
 * status (use PATCH /students/manage/{id}/status, which already exists).
 */
public record UpdateStudentRequest(
        @NotBlank String fullName,
        String phone,
        String batch,
        String branch,
        String college,
        Integer graduationYear
) {}
