package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreatePracticeSubmissionRequest;
import com.rrtechnosoft.lms.dto.response.PracticeProblemResponse;
import com.rrtechnosoft.lms.dto.response.PracticeSubmissionResponse;
import com.rrtechnosoft.lms.entity.PracticeProblem;
import com.rrtechnosoft.lms.entity.PracticeSubmission;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.DifficultyLevel;
import com.rrtechnosoft.lms.entity.enums.PracticeTrack;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.PracticeProblemRepository;
import com.rrtechnosoft.lms.repository.PracticeSubmissionRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import com.rrtechnosoft.lms.repository.spec.PracticeProblemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final PracticeProblemRepository practiceProblemRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Page<PracticeProblemResponse> list(UUID studentId, String search, String difficulty, String topic, Pageable pageable) {
        DifficultyLevel difficultyLevel = fromFrontendDifficulty(difficulty);
        PracticeTrack track = parseTrack(topic);

        Page<PracticeProblem> page = practiceProblemRepository.findAll(
                PracticeProblemSpecification.filter(search, difficultyLevel, track), pageable);

        List<UUID> problemIds = page.getContent().stream().map(PracticeProblem::getId).toList();
        if (problemIds.isEmpty()) {
            return page.map(p -> PracticeProblemResponse.from(p, false, 0));
        }

        Map<UUID, PracticeSubmissionRepository.ProblemStats> statsByProblem = practiceSubmissionRepository
                .findStatsForProblems(problemIds).stream()
                .collect(Collectors.toMap(PracticeSubmissionRepository.ProblemStats::getProblemId, s -> s));
        var solvedIds = practiceSubmissionRepository.findSolvedProblemIds(studentId, problemIds);

        return page.map(p -> {
            var stats = statsByProblem.get(p.getId());
            int successRate = 0;
            if (stats != null && stats.getTotalCount() != null && stats.getTotalCount() > 0) {
                long correct = stats.getCorrectCount() != null ? stats.getCorrectCount() : 0L;
                successRate = (int) Math.round((correct * 100.0) / stats.getTotalCount());
            }
            return PracticeProblemResponse.from(p, solvedIds.contains(p.getId()), successRate);
        });
    }

    public PracticeProblemResponse get(UUID studentId, UUID problemId) {
        PracticeProblem problem = practiceProblemRepository.findById(problemId)
                .orElseThrow(() -> ApiException.notFound("Practice problem not found"));

        var stats = practiceSubmissionRepository.findStatsForProblems(List.of(problemId));
        int successRate = 0;
        if (!stats.isEmpty()) {
            var s = stats.get(0);
            if (s.getTotalCount() != null && s.getTotalCount() > 0) {
                long correct = s.getCorrectCount() != null ? s.getCorrectCount() : 0L;
                successRate = (int) Math.round((correct * 100.0) / s.getTotalCount());
            }
        }
        boolean solvedByMe = !practiceSubmissionRepository.findSolvedProblemIds(studentId, List.of(problemId)).isEmpty();

        return PracticeProblemResponse.from(problem, solvedByMe, successRate);
    }

    @Transactional
    public PracticeSubmissionResponse submit(UUID studentId, CreatePracticeSubmissionRequest request) {
        PracticeProblem problem = practiceProblemRepository.findById(request.problemId())
                .orElseThrow(() -> ApiException.notFound("Practice problem not found"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        // NOTE: isCorrect is intentionally left false — there is no sandboxed
        // code-execution engine in this project to grade submissions against
        // testCases (see PracticeSubmission's class comment for why that's
        // out of scope rather than a stub). The frontend submit flow already
        // reflects this: it shows "Submission received", not a verdict.
        PracticeSubmission submission = PracticeSubmission.builder()
                .problem(problem)
                .student(student)
                .code(request.code())
                .language(request.language())
                .build();
        submission = practiceSubmissionRepository.save(submission);

        auditLogService.log(studentId, "SUBMIT_PRACTICE_PROBLEM", "PracticeProblem", problem.getId(), null);

        return PracticeSubmissionResponse.from(submission);
    }

    public List<PracticeSubmissionResponse> mySubmissions(UUID studentId, UUID problemId) {
        return practiceSubmissionRepository.findByProblem_IdAndStudent_IdOrderBySubmittedAtDesc(problemId, studentId)
                .stream()
                .map(PracticeSubmissionResponse::from)
                .toList();
    }

    private DifficultyLevel fromFrontendDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) return null;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> DifficultyLevel.BEGINNER;
            case "MEDIUM" -> DifficultyLevel.INTERMEDIATE;
            case "HARD" -> DifficultyLevel.ADVANCED;
            default -> throw ApiException.badRequest("Unknown difficulty: " + difficulty);
        };
    }

    private PracticeTrack parseTrack(String topic) {
        if (topic == null || topic.isBlank()) return null;
        try {
            return PracticeTrack.valueOf(topic.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown topic: " + topic);
        }
    }
}
