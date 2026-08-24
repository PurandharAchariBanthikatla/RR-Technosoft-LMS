package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.InterviewSchedule;
import com.rrtechnosoft.lms.entity.enums.InterviewMode;
import com.rrtechnosoft.lms.entity.enums.InterviewResult;
import com.rrtechnosoft.lms.entity.enums.InterviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InterviewScheduleResponse(
        UUID id,
        UUID applicationId,
        UUID placementId,
        String companyName,
        String role,
        UUID studentId,
        String studentName,
        Integer roundNumber,
        String roundName,
        OffsetDateTime scheduledAt,
        InterviewMode mode,
        String venueOrLink,
        String interviewerName,
        InterviewStatus status,
        InterviewResult result,
        String feedback,
        OffsetDateTime createdAt
) {
    public static InterviewScheduleResponse from(InterviewSchedule i) {
        var app = i.getApplication();
        return new InterviewScheduleResponse(
                i.getId(), app.getId(), app.getPlacement().getId(), app.getPlacement().getCompanyName(),
                app.getPlacement().getRoleTitle(), app.getStudent().getId(), app.getStudent().getFullName(),
                i.getRoundNumber(), i.getRoundName(), i.getScheduledAt(), i.getMode(), i.getVenueOrLink(),
                i.getInterviewerName(), i.getStatus(), i.getResult(), i.getFeedback(), i.getCreatedAt()
        );
    }
}
