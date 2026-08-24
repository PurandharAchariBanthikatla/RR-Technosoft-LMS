package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/** Batched per-student rollup (course count + avg progress) for the Student Report page. */
public interface StudentEnrollmentAggProjection {
    UUID getStudentId();
    Long getCourseCount();
    Double getAvgProgress();
}
