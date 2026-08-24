package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BulkMarkAttendanceRequest(
        @NotNull UUID courseId,
        @NotNull LocalDate date,
        @NotEmpty @Valid List<Record> records
) {
    public record Record(
            @NotNull UUID studentId,
            @NotNull AttendanceStatus status
    ) {}
}
