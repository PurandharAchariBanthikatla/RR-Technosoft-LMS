package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.config.AppProperties;
import com.rrtechnosoft.lms.dto.request.IssueCertificateRequest;
import com.rrtechnosoft.lms.dto.response.CertificateResponse;
import com.rrtechnosoft.lms.entity.Certificate;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CertificateRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.EnrollmentRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.service.storage.CertificatePdfService;
import com.rrtechnosoft.lms.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CertificatePdfService pdfService;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private CertificateService certificateService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendBaseUrl("https://lms.rrtechnosoft.com");
        certificateService = new CertificateService(certificateRepository, courseRepository, userRepository,
                enrollmentRepository, pdfService, fileStorageService, appProperties, auditLogService, notificationService);
    }

    @Test
    void issue_rejectsWhenCertificateAlreadyExists() {
        when(certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> certificateService.issue(new IssueCertificateRequest(studentId, courseId), actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already been issued");
    }

    @Test
    void issue_rejectsWhenStudentHasNotCompletedCourse() {
        when(certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(eq(studentId), eq(courseId), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> certificateService.issue(new IssueCertificateRequest(studentId, courseId), actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("has not completed");
    }

    @Test
    void issue_generatesPdfUploadsAndSavesCertificate() {
        when(certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(eq(studentId), eq(courseId), any()))
                .thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(Course.builder().id(courseId).title("Cloud Native 101").build()));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().id(studentId).fullName("Priya Menon").build()));
        when(pdfService.generate(any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(fileStorageService.upload(any(), any(), any())).thenReturn("https://s3.example/upload-ack");
        when(fileStorageService.presignedUrl(any())).thenReturn("https://s3.example/presigned");
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> {
            Certificate c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CertificateResponse response = certificateService.issue(new IssueCertificateRequest(studentId, courseId), actorId);

        assertThat(response.studentName()).isEqualTo("Priya Menon");
        assertThat(response.courseTitle()).isEqualTo("Cloud Native 101");
        assertThat(response.certificateUrl()).isEqualTo("https://s3.example/presigned");
        assertThat(response.verificationCode()).startsWith("RRT-");
        verify(auditLogService).log(eq(actorId), eq("ISSUE_CERTIFICATE"), eq("Certificate"), any(), isNull());
        verify(notificationService).notify(eq(studentId), any(), any(), any(), any());
    }

    @Test
    void verify_throwsNotFoundForUnknownCode() {
        when(certificateRepository.findByCertificateNo("BOGUS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.verify("BOGUS"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No certificate found");
    }
}
