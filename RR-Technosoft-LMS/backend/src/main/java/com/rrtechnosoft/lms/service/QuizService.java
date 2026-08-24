package com.rrtechnosoft.lms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrtechnosoft.lms.dto.request.CreateQuizRequest;
import com.rrtechnosoft.lms.dto.request.SubmitQuizRequest;
import com.rrtechnosoft.lms.dto.response.QuizAttemptResultResponse;
import com.rrtechnosoft.lms.dto.response.QuizQuestionResponse;
import com.rrtechnosoft.lms.dto.response.QuizResponse;
import com.rrtechnosoft.lms.dto.shared.QuestionOption;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // ---- list / get -----------------------------------------------------

    @Transactional(readOnly = true)
    public Page<QuizResponse> listForAdmin(UUID courseId, Pageable pageable) {
        Page<Quiz> page = quizRepository.search(courseId, pageable);
        if (page.isEmpty()) return page.map(q -> QuizResponse.of(q, 0, null, null));

        List<UUID> quizIds = page.getContent().stream().map(Quiz::getId).toList();
        Map<UUID, Long> questionCounts = toCountMap(quizQuestionRepository.countByQuizIds(quizIds));

        return page.map(q -> QuizResponse.of(q, questionCounts.getOrDefault(q.getId(), 0L), null, null));
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> listForStudent(UUID courseId, UUID studentId, Pageable pageable) {
        Page<Quiz> page = quizRepository.search(courseId, pageable);
        if (page.isEmpty()) return page.map(q -> QuizResponse.of(q, 0, false, null));

        List<UUID> quizIds = page.getContent().stream().map(Quiz::getId).toList();
        Map<UUID, Long> questionCounts = toCountMap(quizQuestionRepository.countByQuizIds(quizIds));

        Map<UUID, QuizAttempt> attempts = quizAttemptRepository.findByQuizIdsAndStudentId(quizIds, studentId).stream()
                .collect(Collectors.toMap(a -> a.getQuiz().getId(), a -> a));

        return page.map(q -> {
            long total = questionCounts.getOrDefault(q.getId(), 0L);
            QuizAttempt attempt = attempts.get(q.getId());
            boolean attempted = attempt != null && attempt.getSubmittedAt() != null;
            Integer score = attempted ? marksFromPct(attempt.getScorePct(), total) : null;
            return QuizResponse.of(q, total, attempted, score);
        });
    }

    @Transactional(readOnly = true)
    public QuizResponse get(UUID id, UUID viewerId, boolean isStudent) {
        Quiz quiz = quizRepository.findByIdWithCourse(id)
                .orElseThrow(() -> ApiException.notFound("Quiz not found"));
        long total = quizQuestionRepository.countByQuizId(id);
        if (!isStudent) {
            return QuizResponse.of(quiz, total, null, null);
        }
        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(id, viewerId).orElse(null);
        boolean attempted = attempt != null && attempt.getSubmittedAt() != null;
        Integer score = attempted ? marksFromPct(attempt.getScorePct(), total) : null;
        return QuizResponse.of(quiz, total, attempted, score);
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getQuestions(UUID quizId, boolean includeAnswerKey) {
        if (!quizRepository.existsById(quizId)) {
            throw ApiException.notFound("Quiz not found");
        }
        return quizQuestionRepository.findByQuizIdOrderByPositionAsc(quizId).stream()
                .map(q -> toQuestionResponse(q, includeAnswerKey))
                .toList();
    }

    // ---- create -----------------------------------------------------------

    @Transactional
    public QuizResponse create(CreateQuizRequest request, UUID actorId) {
        if (!request.availableTo().isAfter(request.availableFrom())) {
            throw ApiException.badRequest("availableTo must be after availableFrom");
        }
        CourseModule module = request.moduleId() == null ? null : courseModuleRepository.findById(request.moduleId())
                .orElseThrow(() -> ApiException.notFound("Module not found"));

        Quiz quiz = Quiz.builder()
                .module(module)
                .title(request.title())
                .timeLimitMinutes(request.durationMinutes())
                .passScorePct(request.passScorePct())
                .availableFrom(request.availableFrom())
                .availableTo(request.availableTo())
                .createdBy(actorId)
                .build();
        quiz = quizRepository.save(quiz);

        int position = 0;
        for (CreateQuizRequest.QuestionInput input : request.questions()) {
            if (input.correctOptionIndex() >= input.options().size()) {
                throw ApiException.badRequest("correctOptionIndex out of range for question: " + input.question());
            }
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < input.options().size(); i++) {
                options.add(new QuestionOption(String.valueOf((char) ('A' + i)), input.options().get(i)));
            }
            String correctKey = String.valueOf((char) ('A' + input.correctOptionIndex()));

            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .question(input.question())
                    .options(writeJson(options))
                    .correctOption(correctKey)
                    .position(position++)
                    .build();
            quizQuestionRepository.save(question);
        }

        auditLogService.log(actorId, "CREATE_QUIZ", "Quiz", quiz.getId(), null);
        return QuizResponse.of(quiz, request.questions().size(), null, null);
    }

    // ---- submit -------------------------------------------------------------

    @Transactional
    public QuizAttemptResultResponse submit(UUID quizId, SubmitQuizRequest request, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> ApiException.notFound("Quiz not found"));
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(quiz.getAvailableFrom()) || now.isAfter(quiz.getAvailableTo())) {
            throw ApiException.badRequest("Quiz is not currently open for attempts");
        }
        if (quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId).isPresent()) {
            throw ApiException.conflict("Quiz already attempted");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByPositionAsc(quizId);
        if (questions.isEmpty()) {
            throw ApiException.badRequest("Quiz has no questions");
        }
        Map<UUID, QuizQuestion> byId = questions.stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        Map<String, String> answerKeyByQuestionId = new LinkedHashMap<>();
        int correctCount = 0;
        for (SubmitQuizRequest.Answer answer : request.answers()) {
            QuizQuestion question = byId.get(answer.questionId());
            if (question == null) {
                throw ApiException.badRequest("Question not found in this quiz: " + answer.questionId());
            }
            List<QuestionOption> options = readOptions(question.getOptions());
            if (answer.optionIndex() < 0 || answer.optionIndex() >= options.size()) {
                throw ApiException.badRequest("optionIndex out of range for question: " + question.getId());
            }
            String chosenKey = options.get(answer.optionIndex()).key();
            answerKeyByQuestionId.put(question.getId().toString(), chosenKey);
            if (chosenKey.equals(question.getCorrectOption())) {
                correctCount++;
            }
        }

        BigDecimal scorePct = BigDecimal.valueOf(correctCount)
                .divide(BigDecimal.valueOf(questions.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .answers(writeJson(answerKeyByQuestionId))
                .scorePct(scorePct)
                .submittedAt(OffsetDateTime.now())
                .build();
        quizAttemptRepository.save(attempt);
        auditLogService.log(studentId, "SUBMIT_QUIZ", "QuizAttempt", attempt.getId(), null);

        boolean passed = scorePct.compareTo(quiz.getPassScorePct()) >= 0;
        return new QuizAttemptResultResponse(quizId, scorePct, correctCount, questions.size(), passed);
    }

    // ---- helpers -----------------------------------------------------------

    private QuizQuestionResponse toQuestionResponse(QuizQuestion question, boolean includeAnswerKey) {
        List<QuestionOption> options = readOptions(question.getOptions());
        List<String> optionTexts = options.stream().map(QuestionOption::text).toList();
        Integer correctIndex = null;
        if (includeAnswerKey) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).key().equals(question.getCorrectOption())) {
                    correctIndex = i;
                    break;
                }
            }
        }
        return new QuizQuestionResponse(question.getId(), question.getQuestion(), optionTexts, correctIndex);
    }

    private List<QuestionOption> readOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Malformed quiz question options JSON", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quiz JSON payload", e);
        }
    }

    private Integer marksFromPct(BigDecimal scorePct, long totalMarks) {
        if (scorePct == null || totalMarks == 0) return null;
        return scorePct.multiply(BigDecimal.valueOf(totalMarks))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private static Map<UUID, Long> toCountMap(List<IdCountProjection> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (IdCountProjection row : rows) {
            map.put(row.getId(), row.getCnt());
        }
        return map;
    }
}
