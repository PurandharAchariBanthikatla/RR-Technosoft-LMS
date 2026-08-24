package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreatePracticeSubmissionRequest;
import com.rrtechnosoft.lms.entity.PracticeProblem;
import com.rrtechnosoft.lms.entity.PracticeSubmission;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.DifficultyLevel;
import com.rrtechnosoft.lms.entity.enums.PracticeTrack;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.PracticeProblemRepository;
import com.rrtechnosoft.lms.repository.PracticeSubmissionRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    @Mock private PracticeProblemRepository practiceProblemRepository;
    @Mock private PracticeSubmissionRepository practiceSubmissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private PracticeService practiceService;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        practiceService = new PracticeService(practiceProblemRepository, practiceSubmissionRepository, userRepository, auditLogService);
    }

    @Test
    void list_computesSuccessRateAndSolvedByMeFromBatchedStats() {
        UUID problemId = UUID.randomUUID();
        PracticeProblem problem = PracticeProblem.builder()
                .id(problemId).title("Two Sum").track(PracticeTrack.DSA)
                .difficulty(DifficultyLevel.BEGINNER).statement("...").build();

        Pageable pageable = PageRequest.of(0, 20);
        when(practiceProblemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(problem), pageable, 1));

        PracticeSubmissionRepository.ProblemStats stats = mock(PracticeSubmissionRepository.ProblemStats.class);
        when(stats.getProblemId()).thenReturn(problemId);
        when(stats.getTotalCount()).thenReturn(4L);
        when(stats.getCorrectCount()).thenReturn(3L);
        when(practiceSubmissionRepository.findStatsForProblems(List.of(problemId))).thenReturn(List.of(stats));
        when(practiceSubmissionRepository.findSolvedProblemIds(studentId, List.of(problemId))).thenReturn(List.of(problemId));

        var result = practiceService.list(studentId, null, null, null, pageable);

        var dto = result.getContent().get(0);
        assertThat(dto.successRate()).isEqualTo(75);
        assertThat(dto.solvedByMe()).isTrue();
        assertThat(dto.difficulty()).isEqualTo("EASY"); // BEGINNER -> EASY
        assertThat(dto.topic()).isEqualTo("DSA");
    }

    @Test
    void list_rejectsUnknownDifficultyFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> practiceService.list(studentId, null, "NIGHTMARE", null, pageable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown difficulty");
    }

    @Test
    void submit_savesSubmissionWithIsCorrectDefaultedFalse() {
        UUID problemId = UUID.randomUUID();
        PracticeProblem problem = PracticeProblem.builder().id(problemId).title("Two Sum")
                .track(PracticeTrack.DSA).difficulty(DifficultyLevel.BEGINNER).statement("...").build();
        User student = User.builder().id(studentId).fullName("Kiran").build();

        when(practiceProblemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(practiceSubmissionRepository.save(any(PracticeSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CreatePracticeSubmissionRequest(problemId, "python", "print('hi')");
        var response = practiceService.submit(studentId, request);

        assertThat(response.isCorrect()).isFalse();
        assertThat(response.language()).isEqualTo("python");
        verify(auditLogService).log(eq(studentId), eq("SUBMIT_PRACTICE_PROBLEM"), eq("PracticeProblem"), eq(problemId), isNull());
    }

    @Test
    void submit_rejectsUnknownProblem() {
        UUID problemId = UUID.randomUUID();
        when(practiceProblemRepository.findById(problemId)).thenReturn(Optional.empty());

        var request = new CreatePracticeSubmissionRequest(problemId, "python", "code");

        assertThatThrownBy(() -> practiceService.submit(studentId, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
