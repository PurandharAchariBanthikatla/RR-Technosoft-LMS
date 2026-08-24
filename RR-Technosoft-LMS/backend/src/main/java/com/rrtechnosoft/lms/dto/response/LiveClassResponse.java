package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.LiveClass;
import com.rrtechnosoft.lms.entity.enums.LiveClassStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Field names pinned to the frontend `LiveClass` type in src/types/index.ts. */
public record LiveClassResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String title,
        String instructorName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        LiveClassStatus status,
        String meetingUrl,
        String recordingUrl
) {
    public static LiveClassResponse from(LiveClass lc) {
        return new LiveClassResponse(
                lc.getId(),
                lc.getCourse() != null ? lc.getCourse().getId() : null,
                lc.getCourse() != null ? lc.getCourse().getTitle() : null,
                lc.getTitle(),
                lc.getCourse() != null ? lc.getCourse().getInstructorName() : null,
                lc.getScheduledStart(),
                lc.getScheduledEnd(),
                lc.getStatus(),
                lc.getMeetingLink(),
                lc.getRecordingUrl()
        );
    }
}
