package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.PracticeProblem;
import com.rrtechnosoft.lms.entity.enums.DifficultyLevel;

import java.util.UUID;

/**
 * Field names/values are pinned to the frontend's PracticeProblem type
 * (rr-technosoft-lms/src/types/index.ts) rather than the DB's naming:
 *  - "difficulty" is exposed as EASY/MEDIUM/HARD (frontend's ProblemDifficulty),
 *    translated from the DB's BEGINNER/INTERMEDIATE/ADVANCED (difficulty_level
 *    enum) — see {@link #toFrontendDifficulty}. Kept as a response-side mapping
 *    rather than changing the DB enum, since the schema's difficulty_level
 *    type is also used consistently with that vocabulary elsewhere.
 *  - "topic" is the DB's `track` enum, rendered as-is (the frontend treats it
 *    as an opaque display string, so no translation needed there).
 */
public record PracticeProblemResponse(
        UUID id,
        String title,
        String difficulty,
        String topic,
        boolean solvedByMe,
        int successRate,
        String description
) {
    public static PracticeProblemResponse from(PracticeProblem p, boolean solvedByMe, int successRate) {
        return new PracticeProblemResponse(
                p.getId(),
                p.getTitle(),
                toFrontendDifficulty(p.getDifficulty()),
                p.getTrack().name(),
                solvedByMe,
                successRate,
                p.getStatement()
        );
    }

    private static String toFrontendDifficulty(DifficultyLevel level) {
        return switch (level) {
            case BEGINNER -> "EASY";
            case INTERMEDIATE -> "MEDIUM";
            case ADVANCED -> "HARD";
        };
    }
}
