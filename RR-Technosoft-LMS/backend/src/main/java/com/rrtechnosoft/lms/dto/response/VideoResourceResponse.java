package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.VideoResource;
import com.rrtechnosoft.lms.entity.enums.VideoSource;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VideoResourceResponse(
        UUID id,
        String title,
        String description,
        String category,
        UUID courseId,
        VideoSource source,
        String videoUrl,
        String thumbnailUrl,
        Integer durationSeconds,
        Long fileSizeBytes,
        Boolean isPublished,
        Long viewCount,
        UUID uploadedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static VideoResourceResponse from(VideoResource v) {
        return new VideoResourceResponse(
                v.getId(), v.getTitle(), v.getDescription(), v.getCategory(), v.getCourseId(), v.getSource(),
                v.getVideoUrl(), v.getThumbnailUrl(), v.getDurationSeconds(), v.getFileSizeBytes(),
                v.getIsPublished(), v.getViewCount(), v.getUploadedBy(), v.getCreatedAt(), v.getUpdatedAt()
        );
    }
}
