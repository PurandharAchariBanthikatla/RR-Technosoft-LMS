package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Lesson;
import com.rrtechnosoft.lms.entity.enums.ContentType;

import java.util.UUID;

/**
 * Frontend `LessonType` is the narrower VIDEO | ARTICLE | RESOURCE set (see
 * src/types/index.ts). The schema's ContentType is broader (PDF, QUIZ,
 * ASSIGNMENT, CODING_LAB, PROJECT) for other modules to reuse later; this
 * DTO collapses anything that isn't VIDEO/ARTICLE down to RESOURCE so the
 * Courses module's public contract stays exactly what the frontend expects.
 *
 * `completed` always comes back false here — real per-student completion
 * lives in lesson_progress, which ships with the Enrollments module.
 */
public record LessonResponse(
        UUID id,
        UUID moduleId,
        String title,
        String type,
        Integer durationMinutes,
        int order,
        boolean completed,
        String contentUrl
) {
    public static LessonResponse from(Lesson l) {
        String type = switch (l.getContentType()) {
            case VIDEO -> "VIDEO";
            case ARTICLE -> "ARTICLE";
            default -> "RESOURCE";
        };
        String contentUrl = switch (l.getContentType()) {
            case VIDEO -> l.getVideoUrl();
            case PDF -> l.getPdfUrl();
            default -> null;
        };
        return new LessonResponse(
                l.getId(), l.getModule().getId(), l.getTitle(), type,
                l.getDurationMinutes(), l.getPosition(), false, contentUrl
        );
    }

    /** Maps the frontend's narrow type back to the schema's ContentType for persistence. */
    public static ContentType toContentType(String frontendType) {
        return switch (frontendType) {
            case "VIDEO" -> ContentType.VIDEO;
            case "ARTICLE" -> ContentType.ARTICLE;
            case "RESOURCE" -> ContentType.PDF;
            default -> throw new IllegalArgumentException("Unknown lesson type: " + frontendType);
        };
    }
}
