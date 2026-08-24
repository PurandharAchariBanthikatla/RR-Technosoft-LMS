package com.rrtechnosoft.lms.repository;

import java.math.BigDecimal;

/** Batched per-instructor rollup (total enrollments, avg completion, revenue) for the Faculty Report page. */
public interface FacultyEnrollmentStatsProjection {
    String getInstructorName();
    Long getTotalEnrollments();
    Double getAvgCompletion();
    BigDecimal getRevenue();
}
