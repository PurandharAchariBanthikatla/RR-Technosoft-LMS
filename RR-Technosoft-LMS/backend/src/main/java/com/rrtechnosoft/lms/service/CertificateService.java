package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.config.AppProperties;
import com.rrtechnosoft.lms.dto.request.IssueCertificateRequest;
import com.rrtechnosoft.lms.dto.response.CertificateResponse;
import com.rrtechnosoft.lms.entity.Certificate;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.entity.enums.NotificationType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CertificateRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.EnrollmentRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.service.storage.CertificatePdfService;
import com.rrtechnosoft.lms.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificatePdfService pdfService;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private static final DateTimeFormatter NO_SUFFIX = DateTimeFormatter.ofPattern("yyyyMM");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<CertificateResponse> list(Pageable pageable) {
        return certificateRepository.findAllBy(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> mine(UUID studentId) {
        return certificateRepository.findByStudentIdOrderByIssuedAtDesc(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse verify(String certificateNo) {
        Certificate certificate = certificateRepository.findByCertificateNo(certificateNo)
                .orElseThrow(() -> ApiException.notFound("No certificate found for this code"));
        return toResponse(certificate);
    }

    @Transactional
    public CertificateResponse issue(IssueCertificateRequest request, UUID actorId) {
        if (certificateRepository.existsByStudentIdAndCourseId(request.studentId(), request.courseId())) {
            throw ApiException.conflict("A certificate has already been issued for this student/course");
        }
        boolean completed = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                request.studentId(), request.courseId(), EnrollmentStatus.COMPLETED);
        if (!completed) {
            throw ApiException.badRequest("Student has not completed this course yet");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        String certificateNo = generateCertificateNo();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        String verificationUrl = appProperties.getFrontendBaseUrl() + "/verify-certificate/" + certificateNo;

        byte[] pdf = pdfService.generate(student.getFullName(), course.getTitle(), certificateNo, issuedAt, verificationUrl);
        String fileKey = "certificates/" + certificateNo + ".pdf";
        fileStorageService.upload(fileKey, pdf, "application/pdf");

        Certificate certificate = Certificate.builder()
                .student(student)
                .course(course)
                .certificateNo(certificateNo)
                .fileUrl(fileKey)
                .issuedBy(actorId)
                .build();
        certificate = certificateRepository.save(certificate);
        auditLogService.log(actorId, "ISSUE_CERTIFICATE", "Certificate", certificate.getId(), null);
        notificationService.notify(student.getId(), NotificationType.CERTIFICATE,
                "Your certificate is ready",
                "Your certificate for " + course.getTitle() + " has been issued. Certificate No: " + certificateNo,
                "/student/certificates");
        return toResponse(certificate);
    }

    private CertificateResponse toResponse(Certificate certificate) {
        String url = certificate.getFileUrl() != null ? fileStorageService.presignedUrl(certificate.getFileUrl()) : null;
        return CertificateResponse.of(certificate, url);
    }

    private String generateCertificateNo() {
        String prefix = "RRT-" + OffsetDateTime.now().format(NO_SUFFIX) + "-";
        String suffix = String.format("%06d", RANDOM.nextInt(1_000_000));
        return prefix + suffix;
    }
}
