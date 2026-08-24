package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MarkAttendanceRequest(
        @NotNull UUID studentId,
        @NotNull UUID courseId,
        @NotNull LocalDate date,
        @NotNull AttendanceStatus status
) {}
