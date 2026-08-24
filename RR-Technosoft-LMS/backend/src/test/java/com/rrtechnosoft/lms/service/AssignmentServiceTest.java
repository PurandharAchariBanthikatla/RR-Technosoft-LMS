package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.GradeSubmissionRequest;
import com.rrtechnosoft.lms.dto.request.SubmitAssignmentRequest;
import com.rrtechnosoft.lms.dto.response.AssignmentResponse;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.SubmissionStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    @InjectMocks private AssignmentService assignmentService;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    private Assignment assignmentDueYesterday() {
        return Assignment.builder().id(assignmentId).title("Capstone").maxScore(100)
                .dueAt(OffsetDateTime.now().minusDays(1)).build();
    }

    @Test
    void submit_marksLateWhenPastDueDate() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignmentDueYesterday()));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().id(studentId).fullName("Neha").build()));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        AssignmentResponse response = assignmentService.submit(assignmentId, new SubmitAssignmentRequest(null, "my answer"), studentId);

        assertThat(response.status()).isEqualTo(SubmissionStatus.LATE);
    }

    @Test
    void submit_rejectsEmptySubmission() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignmentDueYesterday()));

        assertThatThrownBy(() -> assignmentService.submit(assignmentId, new SubmitAssignmentRequest(null, null), studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must include a file or text");
    }

    @Test
    void grade_rejectsScoreAboveMaxScore() {
        Assignment assignment = Assignment.builder().id(assignmentId).maxScore(50).build();
        AssignmentSubmission submission = AssignmentSubmission.builder().id(UUID.randomUUID()).assignment(assignment).build();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> assignmentService.grade(assignmentId, submission.getId(),
                new GradeSubmissionRequest(75, "great work"), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot exceed maxScore");
    }

    @Test
    void grade_setsStatusGradedOnValidScore() {
        Assignment assignment = Assignment.builder().id(assignmentId).maxScore(100).build();
        AssignmentSubmission submission = AssignmentSubmission.builder().id(UUID.randomUUID()).assignment(assignment)
                .student(User.builder().id(studentId).build())
                .status(SubmissionStatus.SUBMITTED).build();
        UUID graderId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = assignmentService.grade(assignmentId, submission.getId(),
                new GradeSubmissionRequest(88, "great work"), graderId);

        assertThat(response.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(response.score()).isEqualTo(88);
        assertThat(submission.getGradedBy()).isEqualTo(graderId);
    }
}
