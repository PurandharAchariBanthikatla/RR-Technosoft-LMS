package com.rrtechnosoft.lms.dto.response.reports;

/**
 * Dashboard "course category distribution" chart — one row per course
 * category. Built directly by a JPQL constructor-expression in
 * CourseRepository#courseDistributionRaw, so the constructor signature
 * (String, Long, Long) must match that query's select list exactly.
 */
public record CourseDistributionResponse(
        String category,
        Long courseCount,
        Long enrollmentCount
) {}
