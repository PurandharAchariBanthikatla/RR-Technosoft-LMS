package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateVideoResourceRequest;
import com.rrtechnosoft.lms.dto.request.UpdateVideoResourceRequest;
import com.rrtechnosoft.lms.dto.response.FileUploadResponse;
import com.rrtechnosoft.lms.dto.response.VideoResourceResponse;
import com.rrtechnosoft.lms.entity.VideoResource;
import com.rrtechnosoft.lms.entity.enums.VideoSource;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.VideoResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoResourceService {

    private static final Set<String> ALLOWED_TYPES = Set.of("video/");

    private final VideoResourceRepository videoResourceRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<VideoResourceResponse> list(String search, String category, UUID courseId,
                                             boolean requesterIsStudent, Pageable pageable) {
        return videoResourceRepository
                .search(blankToNull(search), blankToNull(category), courseId, requesterIsStudent, pageable)
                .map(VideoResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public VideoResourceResponse get(UUID id, boolean requesterIsStudent) {
        VideoResource video = findOrThrow(id);
        if (requesterIsStudent && !Boolean.TRUE.equals(video.getIsPublished())) {
            throw ApiException.notFound("Video not found");
        }
        return VideoResourceResponse.from(video);
    }

    @Transactional
    public VideoResourceResponse create(CreateVideoResourceRequest request, UUID actorId) {
        if (request.source() != VideoSource.UPLOAD && (request.videoUrl() == null || request.videoUrl().isBlank())) {
            throw ApiException.badRequest("videoUrl is required for YOUTUBE/EXTERNAL sources");
        }
        VideoResource video = VideoResource.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .courseId(request.courseId())
                .source(request.source())
                .videoUrl(request.videoUrl() != null ? request.videoUrl() : "")
                .thumbnailUrl(request.thumbnailUrl())
                .durationSeconds(request.durationSeconds())
                .isPublished(request.isPublished() == null || request.isPublished())
                .viewCount(0L)
                .uploadedBy(actorId)
                .build();
        video = videoResourceRepository.save(video);
        auditLogService.log(actorId, "VIDEO_RESOURCE_CREATED", "VideoResource", video.getId(), null);
        return VideoResourceResponse.from(video);
    }

    @Transactional
    public VideoResourceResponse update(UUID id, UpdateVideoResourceRequest request, UUID actorId) {
        VideoResource video = findOrThrow(id);
        if (request.source() != VideoSource.UPLOAD && (request.videoUrl() == null || request.videoUrl().isBlank())) {
            throw ApiException.badRequest("videoUrl is required for YOUTUBE/EXTERNAL sources");
        }
        video.setTitle(request.title());
        video.setDescription(request.description());
        video.setCategory(request.category());
        video.setCourseId(request.courseId());
        video.setSource(request.source());
        if (request.videoUrl() != null && !request.videoUrl().isBlank()) {
            video.setVideoUrl(request.videoUrl());
        }
        video.setThumbnailUrl(request.thumbnailUrl());
        video.setDurationSeconds(request.durationSeconds());
        if (request.isPublished() != null) {
            video.setIsPublished(request.isPublished());
        }
        video = videoResourceRepository.save(video);
        auditLogService.log(actorId, "VIDEO_RESOURCE_UPDATED", "VideoResource", id, null);
        return VideoResourceResponse.from(video);
    }

    @Transactional
    public VideoResourceResponse attachFile(UUID id, MultipartFile file, UUID actorId) {
        VideoResource video = findOrThrow(id);
        if (video.getVideoKey() != null) {
            fileStorageService.delete(video.getVideoKey());
        }
        FileUploadResponse uploaded = fileStorageService.upload(file, "video-library", ALLOWED_TYPES);
        video.setSource(VideoSource.UPLOAD);
        video.setVideoUrl(uploaded.fileUrl());
        video.setVideoKey(uploaded.fileKey());
        video.setFileSizeBytes(uploaded.fileSizeBytes());
        video = videoResourceRepository.save(video);
        auditLogService.log(actorId, "VIDEO_RESOURCE_FILE_UPLOADED", "VideoResource", id, null);
        return VideoResourceResponse.from(video);
    }

    @Transactional
    public void recordView(UUID id) {
        VideoResource video = findOrThrow(id);
        video.setViewCount(video.getViewCount() + 1);
        videoResourceRepository.save(video);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        VideoResource video = findOrThrow(id);
        if (video.getVideoKey() != null) {
            fileStorageService.delete(video.getVideoKey());
        }
        videoResourceRepository.delete(video);
        auditLogService.log(actorId, "VIDEO_RESOURCE_DELETED", "VideoResource", id, null);
    }

    private VideoResource findOrThrow(UUID id) {
        return videoResourceRepository.findById(id).orElseThrow(() -> ApiException.notFound("Video not found"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
