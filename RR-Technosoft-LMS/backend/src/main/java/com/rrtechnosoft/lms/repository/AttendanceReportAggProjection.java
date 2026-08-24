package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/** Attendance Report page-source: one row per course with status counts over the requested date range. */
public interface AttendanceReportAggProjection {
    UUID getCourseId();
    String getCourseTitle();
    Long getSessionsHeld();
    Long getPresentCount();
    Long getAbsentCount();
    Long getLateCount();
    Long getExcusedCount();
}
