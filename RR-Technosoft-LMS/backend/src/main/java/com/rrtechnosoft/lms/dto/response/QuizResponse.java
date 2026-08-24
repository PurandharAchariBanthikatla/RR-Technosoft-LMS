package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Quiz;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Field names pinned to the frontend `Quiz` type in src/types/index.ts.
 * attempted/score are per-viewer: populated for a STUDENT from their own
 * QuizAttempt, always null for ADMIN/SUPER_ADMIN (see QuizService).
 */
public record QuizResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String title,
        Integer durationMinutes,
        long totalQuestions,
        long totalMarks,
        Boolean attempted,
        Integer score,
        OffsetDateTime availableFrom,
        OffsetDateTime availableTo
) {
    public static QuizResponse of(Quiz quiz, long totalQuestions, Boolean attempted, Integer score) {
        var module = quiz.getModule();
        var course = module != null ? module.getCourse() : null;
        return new QuizResponse(
                quiz.getId(),
                course != null ? course.getId() : null,
                course != null ? course.getTitle() : null,
                quiz.getTitle(),
                quiz.getTimeLimitMinutes(),
                totalQuestions,
                totalQuestions,
                attempted,
                score,
                quiz.getAvailableFrom(),
                quiz.getAvailableTo()
        );
    }
}
