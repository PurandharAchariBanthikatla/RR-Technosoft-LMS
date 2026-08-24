package com.rrtechnosoft.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrtechnosoft.lms.dto.request.SubmitQuizRequest;
import com.rrtechnosoft.lms.dto.response.QuizAttemptResultResponse;
import com.rrtechnosoft.lms.dto.response.QuizQuestionResponse;
import com.rrtechnosoft.lms.entity.Quiz;
import com.rrtechnosoft.lms.entity.QuizAttempt;
import com.rrtechnosoft.lms.entity.QuizQuestion;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private QuizService quizService;

    private final UUID quizId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID q1Id = UUID.randomUUID();
    private final UUID q2Id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepository, quizQuestionRepository, quizAttemptRepository,
                courseModuleRepository, userRepository, auditLogService, new ObjectMapper());
    }

    private Quiz openQuiz() {
        return Quiz.builder().id(quizId).title("Midterm")
                .passScorePct(BigDecimal.valueOf(60))
                .availableFrom(OffsetDateTime.now().minusDays(1))
                .availableTo(OffsetDateTime.now().plusDays(1))
                .build();
    }

    private List<QuizQuestion> twoQuestions() {
        String opts = "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]";
        QuizQuestion q1 = QuizQuestion.builder().id(q1Id).question("Is Java a language?")
                .options(opts).correctOption("A").position(0).build();
        QuizQuestion q2 = QuizQuestion.builder().id(q2Id).question("Is water dry?")
                .options(opts).correctOption("B").position(1).build();
        return List.of(q1, q2);
    }

    @Test
    void submit_scoresCorrectlyAndDeterminesPassFail() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(openQuiz()));
        when(quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)).thenReturn(Optional.empty());
        when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().id(studentId).build()));
        when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(quizId)).thenReturn(twoQuestions());
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new SubmitQuizRequest(List.of(
                new SubmitQuizRequest.Answer(q1Id, 0),  // correct (A)
                new SubmitQuizRequest.Answer(q2Id, 0)   // wrong (chose A, correct is B)
        ));

        QuizAttemptResultResponse result = quizService.submit(quizId, request, studentId);

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.totalMarks()).isEqualTo(2);
        assertThat(result.scorePct()).isEqualByComparingTo("50.00");
        assertThat(result.passed()).isFalse();
    }

    @Test
    void submit_rejectsSecondAttempt() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(openQuiz()));
        when(quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId))
                .thenReturn(Optional.of(QuizAttempt.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> quizService.submit(quizId, new SubmitQuizRequest(List.of(new SubmitQuizRequest.Answer(q1Id, 0))), studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already attempted");
    }

    @Test
    void submit_rejectsOutsideAvailabilityWindow() {
        Quiz closedQuiz = Quiz.builder().id(quizId).passScorePct(BigDecimal.valueOf(60))
                .availableFrom(OffsetDateTime.now().minusDays(10))
                .availableTo(OffsetDateTime.now().minusDays(1)).build();
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(closedQuiz));

        assertThatThrownBy(() -> quizService.submit(quizId, new SubmitQuizRequest(List.of(new SubmitQuizRequest.Answer(q1Id, 0))), studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not currently open");
    }

    @Test
    void getQuestions_hidesAnswerKeyForStudents() {
        when(quizRepository.existsById(quizId)).thenReturn(true);
        when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(quizId)).thenReturn(twoQuestions());

        List<QuizQuestionResponse> studentView = quizService.getQuestions(quizId, false);
        List<QuizQuestionResponse> adminView = quizService.getQuestions(quizId, true);

        assertThat(studentView).allSatisfy(q -> assertThat(q.correctOptionIndex()).isNull());
        assertThat(adminView).allSatisfy(q -> assertThat(q.correctOptionIndex()).isNotNull());
        assertThat(adminView.get(0).correctOptionIndex()).isEqualTo(0); // "A"
        assertThat(adminView.get(1).correctOptionIndex()).isEqualTo(1); // "B"
    }
}
