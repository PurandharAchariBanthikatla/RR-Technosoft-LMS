package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateLiveClassRequest;
import com.rrtechnosoft.lms.dto.request.UpdateLiveClassRequest;
import com.rrtechnosoft.lms.dto.response.LiveClassResponse;
import com.rrtechnosoft.lms.entity.enums.LiveClassStatus;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.LiveClassService;
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

@RestController
@RequestMapping("/live-classes")
@RequiredArgsConstructor
public class LiveClassController {

    private final LiveClassService liveClassService;

    @GetMapping
    public ResponseEntity<Page<LiveClassResponse>> list(@RequestParam(required = false) LiveClassStatus status,
                                                          @RequestParam(required = false) UUID courseId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(liveClassService.list(status, courseId, pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<LiveClassResponse>> upcoming() {
        return ResponseEntity.ok(liveClassService.upcoming());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LiveClassResponse> create(@Valid @RequestBody CreateLiveClassRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        LiveClassResponse created = liveClassService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LiveClassResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateLiveClassRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(liveClassService.update(id, request, principal.getId()));
    }
}
