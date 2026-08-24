package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.enums.UserRole;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        UserRole role,
        String fullName,
        String email,
        String studentId
) {}
