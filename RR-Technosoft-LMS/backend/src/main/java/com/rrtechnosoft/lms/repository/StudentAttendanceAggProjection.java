package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/** Batched per-student rollup (total sessions + present sessions) for the Student Report page. */
public interface StudentAttendanceAggProjection {
    UUID getStudentId();
    Long getTotalSessions();
    Long getPresentSessions();
}
