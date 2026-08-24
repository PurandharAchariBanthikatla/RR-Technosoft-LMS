package com.rrtechnosoft.lms.entity.enums;

/**
 * Full breadth of lesson content the schema supports. The Courses module's
 * public API only exposes a VIDEO / ARTICLE / RESOURCE subset today (see
 * LessonResponse / CreateLessonRequest) — QUIZ, ASSIGNMENT and CODING_LAB
 * exist here for the Quizzes / Assignments / Practice modules to reuse
 * later without another migration.
 */
public enum ContentType {
    VIDEO,
    PDF,
    ARTICLE,
    QUIZ,
    ASSIGNMENT,
    CODING_LAB,
    PROJECT
}
