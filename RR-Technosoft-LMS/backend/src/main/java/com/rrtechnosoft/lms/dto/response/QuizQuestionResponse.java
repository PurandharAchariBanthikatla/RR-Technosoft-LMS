package com.rrtechnosoft.lms.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * correctOptionIndex is only populated for ADMIN/SUPER_ADMIN viewers
 * (QuizService#toQuestionResponses) — a student taking the quiz must
 * never receive the answer key alongside the question.
 */
public record QuizQuestionResponse(
        UUID id,
        String question,
        List<String> options,
        Integer correctOptionIndex
) {}
