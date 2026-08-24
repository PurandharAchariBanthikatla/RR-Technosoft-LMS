package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateCourseModuleRequest;
import com.rrtechnosoft.lms.dto.request.CreateCourseRequest;
import com.rrtechnosoft.lms.dto.request.UpdateCourseRequest;
import com.rrtechnosoft.lms.dto.response.CourseModuleResponse;
import com.rrtechnosoft.lms.dto.response.CourseResponse;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.CourseService;
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
 * Read endpoints are open to any authenticated user (students see the
 * published catalog only — enforced in CourseService, not here). Writes are
 * restricted to ADMIN/SUPER_ADMIN via method-level @PreAuthorize.
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<Page<CourseResponse>> list(@RequestParam(required = false) String search,
                                                       @RequestParam(required = false) CourseStatus status,
                                                       @RequestParam(required = false) String category,
                                                       @AuthenticationPrincipal UserPrincipal principal,
                                                       @PageableDefault(size = 20) Pageable pageable) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(courseService.listCourses(search, status, category, isStudent, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(courseService.getCourse(id, isStudent));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        CourseResponse created = courseService.createCourse(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CourseResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateCourseRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(courseService.updateCourse(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CourseResponse> setStatus(@PathVariable UUID id,
                                                      @RequestParam CourseStatus status,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(courseService.setCourseStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        courseService.deleteCourse(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------- Modules

    @GetMapping("/{courseId}/modules")
    public ResponseEntity<List<CourseModuleResponse>> listModules(@PathVariable UUID courseId,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(courseService.listModules(courseId, isStudent));
    }

    @PostMapping("/{courseId}/modules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CourseModuleResponse> createModule(@PathVariable UUID courseId,
                                                               @Valid @RequestBody CreateCourseModuleRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        CourseModuleResponse created = courseService.createModule(courseId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
