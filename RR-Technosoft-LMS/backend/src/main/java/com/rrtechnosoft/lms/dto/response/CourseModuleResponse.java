package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.CourseModule;

import java.util.UUID;

public record CourseModuleResponse(
        UUID id,
        UUID courseId,
        String title,
        int order,
        long lessonCount
) {
    public static CourseModuleResponse from(CourseModule m, long lessonCount) {
        return new CourseModuleResponse(m.getId(), m.getCourse().getId(), m.getTitle(), m.getPosition(), lessonCount);
    }
}
