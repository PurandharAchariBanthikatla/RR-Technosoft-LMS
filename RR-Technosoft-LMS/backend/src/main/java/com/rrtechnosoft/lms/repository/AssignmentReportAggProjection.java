package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/** Batched per-assignment rollup (submitted/graded/late counts + avg score) for the Assignment Report page. */
public interface AssignmentReportAggProjection {
    UUID getAssignmentId();
    Long getSubmittedCount();
    Long getGradedCount();
    Long getLateCount();
    Double getAvgScore();
}
