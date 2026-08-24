package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateAssignmentRequest;
import com.rrtechnosoft.lms.dto.request.GradeSubmissionRequest;
import com.rrtechnosoft.lms.dto.request.SubmitAssignmentRequest;
import com.rrtechnosoft.lms.dto.response.AssignmentResponse;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.entity.enums.NotificationType;
import com.rrtechnosoft.lms.entity.enums.SubmissionStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private static final List<EnrollmentStatus> ENROLLED_STATUSES = List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listForAdmin(UUID courseId, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.search(courseId, pageable);
        if (page.isEmpty()) return page.map(a -> AssignmentResponse.forAdmin(a, 0, 0L));

        List<UUID> assignmentIds = page.getContent().stream().map(Assignment::getId).toList();
        List<UUID> courseIds = page.getContent().stream()
                .map(Assignment::getCourse).filter(java.util.Objects::nonNull).map(Course::getId).distinct().toList();

        Map<UUID, Long> submitted = toCountMap(submissionRepository.countByAssignmentIds(assignmentIds));
        Map<UUID, Long> totalStudents = courseIds.isEmpty()
                ? Map.of()
                : toCountMap(enrollmentRepository.countByCourseIdsAndStatuses(courseIds, ENROLLED_STATUSES));

        return page.map(a -> AssignmentResponse.forAdmin(
                a,
                submitted.getOrDefault(a.getId(), 0L),
                a.getCourse() != null ? totalStudents.getOrDefault(a.getCourse().getId(), 0L) : null
        ));
    }

    @Transactional(readOnly = true)
    public Page<AssignmentResponse> listForStudent(UUID courseId, UUID studentId, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.search(courseId, pageable);
        if (page.isEmpty()) return page.map(a -> AssignmentResponse.forStudent(a, null));

        List<UUID> assignmentIds = page.getContent().stream().map(Assignment::getId).toList();
        Map<UUID, AssignmentSubmission> bySubmission = new HashMap<>();
        for (AssignmentSubmission s : submissionRepository.findByAssignmentIdsAndStudentId(assignmentIds, studentId)) {
            bySubmission.put(s.getAssignment().getId(), s);
        }

        return page.map(a -> AssignmentResponse.forStudent(a, bySubmission.get(a.getId())));
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(UUID id, UserPrincipalView viewer) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Assignment not found"));
        if (viewer.isStudent()) {
            AssignmentSubmission submission = submissionRepository
                    .findByAssignmentIdAndStudentId(id, viewer.userId()).orElse(null);
            return AssignmentResponse.forStudent(assignment, submission);
        }
        long submitted = submissionRepository.countByAssignmentIds(List.of(id)).stream()
                .filter(row -> row.getId().equals(id)).mapToLong(IdCountProjection::getCnt).findFirst().orElse(0);
        Long total = assignment.getCourse() == null ? null : enrollmentRepository
                .countByCourseIdsAndStatuses(List.of(assignment.getCourse().getId()), ENROLLED_STATUSES).stream()
                .filter(row -> row.getId().equals(assignment.getCourse().getId()))
                .mapToLong(IdCountProjection::getCnt).findFirst().orElse(0L);
        return AssignmentResponse.forAdmin(assignment, submitted, total);
    }

    /** Minimal viewer shape so the service doesn't depend on the security package's UserPrincipal. */
    public record UserPrincipalView(UUID userId, boolean isStudent) {}

    @Transactional
    public AssignmentResponse create(CreateAssignmentRequest request, UUID actorId) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        CourseModule module = request.moduleId() == null ? null : courseModuleRepository.findById(request.moduleId())
                .orElseThrow(() -> ApiException.notFound("Module not found"));

        Assignment assignment = Assignment.builder()
                .course(course)
                .module(module)
                .title(request.title())
                .instructions(request.description())
                .attachmentUrl(request.attachmentUrl())
                .maxScore(request.maxScore())
                .dueAt(request.dueDate())
                .createdBy(actorId)
                .build();
        assignment = assignmentRepository.save(assignment);
        auditLogService.log(actorId, "CREATE_ASSIGNMENT", "Assignment", assignment.getId(), null);
        return AssignmentResponse.forAdmin(assignment, 0, 0L);
    }

    @Transactional
    public AssignmentResponse submit(UUID assignmentId, SubmitAssignmentRequest request, UUID studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("Assignment not found"));
        if (request.fileUrl() == null && (request.text() == null || request.text().isBlank())) {
            throw ApiException.badRequest("Submission must include a file or text");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> AssignmentSubmission.builder().assignment(assignment).student(student).build());

        OffsetDateTime now = OffsetDateTime.now();
        submission.setSubmissionUrl(request.fileUrl());
        submission.setSubmissionText(request.text());
        submission.setSubmittedAt(now);
        submission.setStatus(assignment.getDueAt() != null && now.isAfter(assignment.getDueAt())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED);

        submission = submissionRepository.save(submission);
        auditLogService.log(studentId, "SUBMIT_ASSIGNMENT", "AssignmentSubmission", submission.getId(), null);
        return AssignmentResponse.forStudent(assignment, submission);
    }

    @Transactional
    public AssignmentResponse grade(UUID assignmentId, UUID submissionId, GradeSubmissionRequest request, UUID actorId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ApiException.notFound("Assignment not found"));
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> ApiException.notFound("Submission not found"));
        if (!submission.getAssignment().getId().equals(assignmentId)) {
            throw ApiException.badRequest("Submission does not belong to this assignment");
        }
        if (request.score() > assignment.getMaxScore()) {
            throw ApiException.badRequest("Score cannot exceed maxScore (" + assignment.getMaxScore() + ")");
        }

        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(OffsetDateTime.now());
        submission.setGradedBy(actorId);
        submission = submissionRepository.save(submission);
        auditLogService.log(actorId, "GRADE_SUBMISSION", "AssignmentSubmission", submission.getId(), null);
        notificationService.notify(submission.getStudent().getId(), NotificationType.ASSIGNMENT,
                "Assignment graded: " + assignment.getTitle(),
                "You scored " + request.score() + "/" + assignment.getMaxScore() + " on \"" + assignment.getTitle() + "\".",
                "/student/assignments");
        return AssignmentResponse.forStudent(assignment, submission);
    }

    private static Map<UUID, Long> toCountMap(List<IdCountProjection> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (IdCountProjection row : rows) {
            map.put(row.getId(), row.getCnt());
        }
        return map;
    }
}
