package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.PlacementApplication;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlacementApplicationResponse(
        UUID id,
        UUID placementId,
        String companyName,
        String role,
        UUID studentId,
        String studentName,
        String studentIdNumber,
        ApplicationStatus status,
        String resumeUrl,
        String notes,
        OffsetDateTime appliedAt,
        OffsetDateTime updatedAt
) {
    public static PlacementApplicationResponse from(PlacementApplication a) {
        return new PlacementApplicationResponse(
                a.getId(), a.getPlacement().getId(), a.getPlacement().getCompanyName(), a.getPlacement().getRoleTitle(),
                a.getStudent().getId(), a.getStudent().getFullName(), a.getStudent().getStudentId(),
                a.getStatus(), a.getResumeUrl(), a.getNotes(), a.getAppliedAt(), a.getUpdatedAt()
        );
    }
}
