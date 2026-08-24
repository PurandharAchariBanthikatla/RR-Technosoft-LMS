package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        UserRole role,
        String email,
        String studentId,
        String fullName,
        String phone,
        AccountStatus status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
    public static UserSummaryResponse from(User u) {
        return new UserSummaryResponse(
                u.getId(), u.getRole(), u.getEmail(), u.getStudentId(), u.getFullName(),
                u.getPhone(), u.getStatus(), u.getLastLoginAt(), u.getCreatedAt()
        );
    }
}
