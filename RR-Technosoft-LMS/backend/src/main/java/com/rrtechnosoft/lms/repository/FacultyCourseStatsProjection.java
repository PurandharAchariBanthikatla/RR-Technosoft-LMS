package com.rrtechnosoft.lms.repository;

/** Faculty Report page-source: one row per distinct instructor name with course count + avg rating. */
public interface FacultyCourseStatsProjection {
    String getInstructorName();
    Long getCoursesHandled();
    Double getAvgRating();
}
