package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.DailyTask;

import java.time.LocalDate;
import java.util.UUID;

/** Field names pinned to the frontend `DailyTask` type in src/types/index.ts. */
public record DailyTaskResponse(
        UUID id,
        String title,
        String description,
        LocalDate date,
        boolean completed,
        String courseTitle
) {
    public static DailyTaskResponse from(DailyTask t, boolean completed) {
        return new DailyTaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getTaskDate(), completed,
                t.getCourse() != null ? t.getCourse().getTitle() : null
        );
    }
}
