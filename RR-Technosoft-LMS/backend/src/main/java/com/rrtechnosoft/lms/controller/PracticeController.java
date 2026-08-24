package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreatePracticeSubmissionRequest;
import com.rrtechnosoft.lms.dto.response.PracticeProblemResponse;
import com.rrtechnosoft.lms.dto.response.PracticeSubmissionResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Restricted to STUDENT via SecurityConfig's existing "/practice/**" rule —
 * no new security matcher needed.
 */
@RestController
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/problems")
    public ResponseEntity<Page<PracticeProblemResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(practiceService.list(principal.getId(), search, difficulty, topic, pageable));
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<PracticeProblemResponse> get(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable UUID id) {
        return ResponseEntity.ok(practiceService.get(principal.getId(), id));
    }

    @PostMapping("/submissions")
    public ResponseEntity<PracticeSubmissionResponse> submit(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody CreatePracticeSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(practiceService.submit(principal.getId(), request));
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<PracticeSubmissionResponse>> mySubmissions(@AuthenticationPrincipal UserPrincipal principal,
                                                                            @RequestParam UUID problemId) {
        return ResponseEntity.ok(practiceService.mySubmissions(principal.getId(), problemId));
    }
}
