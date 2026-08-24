package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Attendance;
import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

/** Field names pinned to the frontend `AttendanceRecord` type in src/types/index.ts. */
public record AttendanceResponse(
        UUID id,
        LocalDate date,
        String courseTitle,
        AttendanceStatus status,
        UUID studentId,
        String studentName
) {
    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(),
                a.getAttendanceDate(),
                a.getCourse().getTitle(),
                a.getStatus(),
                a.getStudent().getId(),
                a.getStudent().getFullName()
        );
    }
}
