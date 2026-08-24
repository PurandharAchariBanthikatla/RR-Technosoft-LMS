package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.enums.CourseLevel;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Field names/shape are pinned to the frontend `Course` type in
 * src/types/index.ts — keep them in sync if either side changes.
 */
public record CourseResponse(
        UUID id,
        String title,
        String slug,
        String description,
        String thumbnailUrl,
        String category,
        CourseLevel level,
        CourseStatus status,
        Integer durationWeeks,
        String instructorName,
        BigDecimal price,
        long studentsEnrolled,
        BigDecimal rating,
        long moduleCount,
        OffsetDateTime createdAt
) {
    public static CourseResponse from(Course c, long moduleCount, long studentsEnrolled) {
        return new CourseResponse(
                c.getId(), c.getTitle(), c.getSlug(), c.getDescription(), c.getThumbnailUrl(),
                c.getCategory(), c.getLevel(), c.getStatus(), c.getDurationWeeks(), c.getInstructorName(),
                c.getPrice(), studentsEnrolled, c.getRating(), moduleCount, c.getCreatedAt()
        );
    }
}
