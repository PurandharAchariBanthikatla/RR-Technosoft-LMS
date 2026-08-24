package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.ApplyPlacementRequest;
import com.rrtechnosoft.lms.dto.request.UpdateApplicationStatusRequest;
import com.rrtechnosoft.lms.dto.response.PlacementApplicationResponse;
import com.rrtechnosoft.lms.entity.Placement;
import com.rrtechnosoft.lms.entity.PlacementApplication;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.PlacementApplicationRepository;
import com.rrtechnosoft.lms.repository.StudentProfileRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rrtechnosoft.lms.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlacementApplicationService {

    private final PlacementApplicationRepository applicationRepository;
    private final PlacementService placementService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    private static final Set<String> RESUME_TYPES = Set.of("application/pdf", "application/msword", "application/vnd.openxmlformats");

    @Transactional
    public PlacementApplicationResponse apply(UUID placementId, ApplyPlacementRequest request, UUID studentId) {
        Placement placement = placementService.findOrThrow(placementId);
        if (placement.getStatus() != PlacementStatus.OPEN) {
            throw ApiException.badRequest("This drive is not currently accepting applications");
        }
        if (applicationRepository.existsByPlacement_IdAndStudent_Id(placementId, studentId)) {
            throw ApiException.conflict("You have already applied to this drive");
        }
        User student = userRepository.findById(studentId).orElseThrow(() -> ApiException.notFound("Student not found"));

        String resumeUrl = request != null ? request.resumeUrl() : null;
        if (resumeUrl == null || resumeUrl.isBlank()) {
            resumeUrl = studentProfileRepository.findByUserId(studentId).map(p -> p.getResumeUrl()).orElse(null);
        }

        PlacementApplication application = PlacementApplication.builder()
                .placement(placement)
                .student(student)
                .status(ApplicationStatus.APPLIED)
                .resumeUrl(resumeUrl)
                .build();
        application = applicationRepository.save(application);
        auditLogService.log(studentId, "PLACEMENT_APPLICATION_SUBMITTED", "PlacementApplication", application.getId(), null);
        return PlacementApplicationResponse.from(application);
    }

    @Transactional
    public void withdraw(UUID applicationId, UUID studentId) {
        PlacementApplication application = findOrThrow(applicationId);
        if (!application.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("You can only withdraw your own application");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
        auditLogService.log(studentId, "PLACEMENT_APPLICATION_WITHDRAWN", "PlacementApplication", applicationId, null);
    }

    @Transactional
    public PlacementApplicationResponse updateStatus(UUID applicationId, UpdateApplicationStatusRequest request, UUID actorId) {
        PlacementApplication application = findOrThrow(applicationId);
        application.setStatus(request.status());
        if (request.notes() != null) {
            application.setNotes(request.notes());
        }
        application = applicationRepository.save(application);
        auditLogService.log(actorId, "PLACEMENT_APPLICATION_STATUS_CHANGED", "PlacementApplication", applicationId, null);
        return PlacementApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public Page<PlacementApplicationResponse> listForPlacement(UUID placementId, ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByPlacement(placementId, status, pageable).map(PlacementApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PlacementApplicationResponse> listForStudent(UUID studentId, Pageable pageable) {
        return applicationRepository.findByStudent_IdOrderByAppliedAtDesc(studentId, pageable).map(PlacementApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public PlacementApplicationResponse get(UUID applicationId) {
        return PlacementApplicationResponse.from(findOrThrow(applicationId));
    }

    @Transactional(readOnly = true)
    public List<PlacementApplication> findByStudentId(UUID studentId) {
        return applicationRepository.findByStudent_Id(studentId);
    }

    @Transactional
    public PlacementApplicationResponse attachResume(UUID applicationId, MultipartFile file, UUID studentId) {
        PlacementApplication application = findOrThrow(applicationId);
        if (!application.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("You can only update your own application");
        }
        FileUploadResponse uploaded = fileStorageService.upload(file, "resumes", RESUME_TYPES);
        application.setResumeUrl(uploaded.fileUrl());
        application = applicationRepository.save(application);
        auditLogService.log(studentId, "PLACEMENT_APPLICATION_RESUME_UPLOADED", "PlacementApplication", applicationId, null);
        return PlacementApplicationResponse.from(application);
    }

    PlacementApplication findOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
    }
}
