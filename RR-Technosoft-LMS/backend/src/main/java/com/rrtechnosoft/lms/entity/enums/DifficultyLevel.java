package com.rrtechnosoft.lms.entity.enums;

/** Matches the `difficulty_level` Postgres enum defined in V1__init_schema.sql
 *  (a separate DB type from `course_level`, despite having the same values). */
public enum DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
