package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateLearningResourceRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        String description,
        @NotNull ResourceType resourceType,
        @Size(max = 100) String category,
        UUID courseId,
        String externalUrl,
        Boolean isPublished
) {}
