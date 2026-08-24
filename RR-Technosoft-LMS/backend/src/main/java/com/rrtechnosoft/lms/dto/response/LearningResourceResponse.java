package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.LearningResource;
import com.rrtechnosoft.lms.entity.enums.ResourceType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LearningResourceResponse(
        UUID id,
        String title,
        String description,
        ResourceType resourceType,
        String category,
        UUID courseId,
        String fileUrl,
        Long fileSizeBytes,
        String externalUrl,
        Boolean isPublished,
        Long downloadCount,
        UUID uploadedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static LearningResourceResponse from(LearningResource r) {
        return new LearningResourceResponse(
                r.getId(), r.getTitle(), r.getDescription(), r.getResourceType(), r.getCategory(), r.getCourseId(),
                r.getFileUrl(), r.getFileSizeBytes(), r.getExternalUrl(), r.getIsPublished(), r.getDownloadCount(),
                r.getUploadedBy(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
