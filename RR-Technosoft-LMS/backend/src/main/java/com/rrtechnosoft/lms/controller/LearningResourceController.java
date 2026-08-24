package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateLearningResourceRequest;
import com.rrtechnosoft.lms.dto.request.UpdateLearningResourceRequest;
import com.rrtechnosoft.lms.dto.response.LearningResourceResponse;
import com.rrtechnosoft.lms.entity.enums.ResourceType;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.LearningResourceService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Learning Resources. Reads open to any authenticated user (students see published only); writes admin-only. */
@RestController
@RequestMapping("/learning-resources")
@RequiredArgsConstructor
public class LearningResourceController {

    private final LearningResourceService learningResourceService;

    @GetMapping
    public ResponseEntity<Page<LearningResourceResponse>> list(@RequestParam(required = false) String search,
                                                                 @RequestParam(required = false) String category,
                                                                 @RequestParam(required = false) ResourceType type,
                                                                 @RequestParam(required = false) UUID courseId,
                                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(learningResourceService.list(search, category, type, courseId, isStudent, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningResourceResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(learningResourceService.get(id, isStudent));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LearningResourceResponse> create(@Valid @RequestBody CreateLearningResourceRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learningResourceService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LearningResourceResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateLearningResourceRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(learningResourceService.update(id, request, principal.getId()));
    }

    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LearningResourceResponse> uploadFile(@PathVariable UUID id,
                                                                 @RequestParam("file") MultipartFile file,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(learningResourceService.attachFile(id, file, principal.getId()));
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<Void> recordDownload(@PathVariable UUID id) {
        learningResourceService.recordDownload(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        learningResourceService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
