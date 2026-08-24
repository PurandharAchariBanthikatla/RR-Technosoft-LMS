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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listEnrollments(EnrollmentStatus status, UUID courseId, Pageable pageable) {
        return enrollmentRepository.search(status, courseId, pageable).map(EnrollmentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional
    public EnrollmentResponse enroll(CreateEnrollmentRequest request, UUID studentId) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw ApiException.badRequest("Course is not open for enrollment");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, request.courseId())) {
            throw ApiException.conflict("Already enrolled in this course");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollment = enrollmentRepository.save(enrollment);
        auditLogService.log(studentId, "CREATE_ENROLLMENT", "Enrollment", enrollment.getId(), null);
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse updateStatus(UUID id, EnrollmentStatus status, UUID actorId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Enrollment not found"));
        enrollment.setStatus(status);
        if (status == EnrollmentStatus.COMPLETED && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(OffsetDateTime.now());
        }
        enrollment = enrollmentRepository.save(enrollment);
        auditLogService.log(actorId, "SET_ENROLLMENT_STATUS_" + status, "Enrollment", id, null);
        return EnrollmentResponse.from(enrollment);
    }
}
