package com.rrtechnosoft.lms.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record QuizAttemptResultResponse(
        UUID quizId,
        BigDecimal scorePct,
        int score,
        long totalMarks,
        boolean passed
) {}
