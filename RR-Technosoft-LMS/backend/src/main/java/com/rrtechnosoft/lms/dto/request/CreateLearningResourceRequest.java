package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * File metadata (fileUrl/fileKey/fileSizeBytes) is set by the service after
 * a successful upload via POST /learning-resources/{id}/file — not part of
 * this request. A resource is created either with an externalUrl (LINK
 * type) or as a file-pending shell that gets its file attached next.
 */
public record CreateLearningResourceRequest(
        @NotBlank @Size(min = 3, max = 200) String title,
        String description,
        @NotNull ResourceType resourceType,
        @Size(max = 100) String category,
        UUID courseId,
        String externalUrl,
        Boolean isPublished
) {}
