package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateQuizRequest;
import com.rrtechnosoft.lms.dto.request.SubmitQuizRequest;
import com.rrtechnosoft.lms.dto.response.QuizAttemptResultResponse;
import com.rrtechnosoft.lms.dto.response.QuizQuestionResponse;
import com.rrtechnosoft.lms.dto.response.QuizResponse;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GET / is viewer-aware (attempted/score populated for students only, see
 * QuizService). GET /{id}/questions never includes the answer key for
 * students — see QuizService#getQuestions / toQuestionResponse.
 */
@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public ResponseEntity<Page<QuizResponse>> list(@RequestParam(required = false) UUID courseId,
                                                     @AuthenticationPrincipal UserPrincipal principal,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        Page<QuizResponse> page = isStudent
                ? quizService.listForStudent(courseId, principal.getId(), pageable)
                : quizService.listForAdmin(courseId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(quizService.get(id, principal.getId(), isStudent));
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuizQuestionResponse>> questions(@PathVariable UUID id,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() != UserRole.STUDENT;
        return ResponseEntity.ok(quizService.getQuestions(id, isAdmin));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<QuizResponse> create(@Valid @RequestBody CreateQuizRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        QuizResponse created = quizService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<QuizAttemptResultResponse> submit(@PathVariable UUID id,
                                                              @Valid @RequestBody SubmitQuizRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(quizService.submit(id, request, principal.getId()));
    }
}
