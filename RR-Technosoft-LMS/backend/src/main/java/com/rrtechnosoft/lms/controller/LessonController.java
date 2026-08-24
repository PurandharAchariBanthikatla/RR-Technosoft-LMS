package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateLessonRequest;
import com.rrtechnosoft.lms.dto.response.LessonResponse;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<LessonResponse>> list(@PathVariable UUID moduleId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(courseService.listLessons(moduleId, isStudent));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LessonResponse> create(@PathVariable UUID moduleId,
                                                  @Valid @RequestBody CreateLessonRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        LessonResponse created = courseService.createLesson(moduleId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
