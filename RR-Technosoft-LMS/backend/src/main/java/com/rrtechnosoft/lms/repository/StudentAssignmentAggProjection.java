package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/** Batched per-student rollup (submitted count + avg score) for the Student Report page. */
public interface StudentAssignmentAggProjection {
    UUID getStudentId();
    Long getSubmittedCount();
    Double getAvgScore();
}
