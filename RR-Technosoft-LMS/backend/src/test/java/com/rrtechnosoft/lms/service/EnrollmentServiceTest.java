package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateEnrollmentRequest;
import com.rrtechnosoft.lms.dto.response.EnrollmentResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.Enrollment;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.EnrollmentRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private EnrollmentService enrollmentService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private Course publishedCourse() {
        Course course = Course.builder().id(courseId).title("AWS Fundamentals").status(CourseStatus.PUBLISHED).build();
        return course;
    }

    private User student() {
        return User.builder().id(studentId).fullName("Asha Rao").build();
    }

    @Test
    void enroll_createsActiveEnrollmentForPublishedCourse() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student()));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EnrollmentResponse response = enrollmentService.enroll(new CreateEnrollmentRequest(courseId), studentId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.studentName()).isEqualTo("Asha Rao");
        verify(auditLogService).log(eq(studentId), eq("CREATE_ENROLLMENT"), eq("Enrollment"), any(), isNull());
    }

    @Test
    void enroll_rejectsUnpublishedCourse() {
        Course draft = Course.builder().id(courseId).title("Draft Course").status(CourseStatus.DRAFT).build();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> enrollmentService.enroll(new CreateEnrollmentRequest(courseId), studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not open for enrollment");
    }

    @Test
    void enroll_rejectsDuplicateEnrollment() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(publishedCourse()));
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(new CreateEnrollmentRequest(courseId), studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Already enrolled");
    }

    @Test
    void updateStatus_settingCompletedStampsCompletedAt() {
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .student(student())
                .course(publishedCourse())
                .status(EnrollmentStatus.ACTIVE)
                .build();
        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse response = enrollmentService.updateStatus(enrollment.getId(), EnrollmentStatus.COMPLETED, studentId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletedAt()).isNotNull();
    }

    @Test
    void updateStatus_throwsWhenEnrollmentMissing() {
        UUID missingId = UUID.randomUUID();
        when(enrollmentRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.updateStatus(missingId, EnrollmentStatus.DROPPED, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
