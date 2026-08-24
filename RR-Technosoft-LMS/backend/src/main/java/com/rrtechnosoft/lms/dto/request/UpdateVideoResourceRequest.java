package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.VideoSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateVideoResourceRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        String description,
        @Size(max = 100) String category,
        UUID courseId,
        @NotNull VideoSource source,
        String videoUrl,
        String thumbnailUrl,
        Integer durationSeconds,
        Boolean isPublished
) {}
