package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateVideoResourceRequest;
import com.rrtechnosoft.lms.dto.request.UpdateVideoResourceRequest;
import com.rrtechnosoft.lms.dto.response.VideoResourceResponse;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.VideoResourceService;
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

/** Video Library. Reads open to any authenticated user (students see published only); writes admin-only. */
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoLibraryController {

    private final VideoResourceService videoResourceService;

    @GetMapping
    public ResponseEntity<Page<VideoResourceResponse>> list(@RequestParam(required = false) String search,
                                                              @RequestParam(required = false) String category,
                                                              @RequestParam(required = false) UUID courseId,
                                                              @AuthenticationPrincipal UserPrincipal principal,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(videoResourceService.list(search, category, courseId, isStudent, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResourceResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(videoResourceService.get(id, isStudent));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VideoResourceResponse> create(@Valid @RequestBody CreateVideoResourceRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(videoResourceService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VideoResourceResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateVideoResourceRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(videoResourceService.update(id, request, principal.getId()));
    }

    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VideoResourceResponse> uploadFile(@PathVariable UUID id,
                                                              @RequestParam("file") MultipartFile file,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(videoResourceService.attachFile(id, file, principal.getId()));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable UUID id) {
        videoResourceService.recordView(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        videoResourceService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
