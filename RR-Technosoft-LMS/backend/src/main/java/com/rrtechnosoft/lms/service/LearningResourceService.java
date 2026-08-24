package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateLearningResourceRequest;
import com.rrtechnosoft.lms.dto.request.UpdateLearningResourceRequest;
import com.rrtechnosoft.lms.dto.response.FileUploadResponse;
import com.rrtechnosoft.lms.dto.response.LearningResourceResponse;
import com.rrtechnosoft.lms.entity.LearningResource;
import com.rrtechnosoft.lms.entity.enums.ResourceType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.LearningResourceRepository;
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
public class LearningResourceService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "application/msword", "application/vnd.openxmlformats",
            "application/vnd.ms-powerpoint", "application/vnd.ms-excel", "text/", "image/", "application/zip");

    private final LearningResourceRepository learningResourceRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<LearningResourceResponse> list(String search, String category, ResourceType type, UUID courseId,
                                                boolean requesterIsStudent, Pageable pageable) {
        return learningResourceRepository
                .search(blankToNull(search), blankToNull(category), type, courseId, requesterIsStudent, pageable)
                .map(LearningResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public LearningResourceResponse get(UUID id, boolean requesterIsStudent) {
        LearningResource resource = findOrThrow(id);
        if (requesterIsStudent && !Boolean.TRUE.equals(resource.getIsPublished())) {
            throw ApiException.notFound("Resource not found");
        }
        return LearningResourceResponse.from(resource);
    }

    @Transactional
    public LearningResourceResponse create(CreateLearningResourceRequest request, UUID actorId) {
        if (request.resourceType() == ResourceType.LINK && (request.externalUrl() == null || request.externalUrl().isBlank())) {
            throw ApiException.badRequest("externalUrl is required for LINK resources");
        }
        LearningResource resource = LearningResource.builder()
                .title(request.title())
                .description(request.description())
                .resourceType(request.resourceType())
                .category(request.category())
                .courseId(request.courseId())
                .externalUrl(request.externalUrl())
                .isPublished(request.isPublished() == null || request.isPublished())
                .downloadCount(0L)
                .uploadedBy(actorId)
                .build();
        resource = learningResourceRepository.save(resource);
        auditLogService.log(actorId, "LEARNING_RESOURCE_CREATED", "LearningResource", resource.getId(), null);
        return LearningResourceResponse.from(resource);
    }

    @Transactional
    public LearningResourceResponse update(UUID id, UpdateLearningResourceRequest request, UUID actorId) {
        LearningResource resource = findOrThrow(id);
        resource.setTitle(request.title());
        resource.setDescription(request.description());
        resource.setResourceType(request.resourceType());
        resource.setCategory(request.category());
        resource.setCourseId(request.courseId());
        resource.setExternalUrl(request.externalUrl());
        if (request.isPublished() != null) {
            resource.setIsPublished(request.isPublished());
        }
        resource = learningResourceRepository.save(resource);
        auditLogService.log(actorId, "LEARNING_RESOURCE_UPDATED", "LearningResource", id, null);
        return LearningResourceResponse.from(resource);
    }

    @Transactional
    public LearningResourceResponse attachFile(UUID id, MultipartFile file, UUID actorId) {
        LearningResource resource = findOrThrow(id);
        // Replacing an existing file: clean up the old S3 object so it doesn't leak.
        if (resource.getFileKey() != null) {
            fileStorageService.delete(resource.getFileKey());
        }
        FileUploadResponse uploaded = fileStorageService.upload(file, "learning-resources", ALLOWED_TYPES);
        resource.setFileUrl(uploaded.fileUrl());
        resource.setFileKey(uploaded.fileKey());
        resource.setFileSizeBytes(uploaded.fileSizeBytes());
        resource = learningResourceRepository.save(resource);
        auditLogService.log(actorId, "LEARNING_RESOURCE_FILE_UPLOADED", "LearningResource", id, null);
        return LearningResourceResponse.from(resource);
    }

    @Transactional
    public void recordDownload(UUID id) {
        LearningResource resource = findOrThrow(id);
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        learningResourceRepository.save(resource);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        LearningResource resource = findOrThrow(id);
        if (resource.getFileKey() != null) {
            fileStorageService.delete(resource.getFileKey());
        }
        learningResourceRepository.delete(resource);
        auditLogService.log(actorId, "LEARNING_RESOURCE_DELETED", "LearningResource", id, null);
    }

    private LearningResource findOrThrow(UUID id) {
        return learningResourceRepository.findById(id).orElseThrow(() -> ApiException.notFound("Resource not found"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
