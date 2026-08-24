package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @NotBlank String fullName,
        String phone,
        // Was @NotBlank only — a 1-character initial password passed validation while
        // CreateAdminRequest enforces min length 8. Matched to the same policy here.
        @NotBlank @Size(min = 8, message = "Initial password must be at least 8 characters") String initialPassword,
        String batch,
        String branch,
        String college,
        Integer graduationYear
        // studentId is server-generated (e.g. RRT2026S0001) — see AdminService.createStudent
) {}
