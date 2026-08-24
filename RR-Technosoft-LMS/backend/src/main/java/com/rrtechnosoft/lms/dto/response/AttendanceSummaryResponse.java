package com.rrtechnosoft.lms.dto.response;

public record AttendanceSummaryResponse(
        long totalClasses,
        long present,
        long absent,
        long late,
        double percentage
) {}
