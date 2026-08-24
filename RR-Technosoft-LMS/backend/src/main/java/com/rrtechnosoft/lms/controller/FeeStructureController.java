package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateFeeStructureRequest;
import com.rrtechnosoft.lms.dto.request.UpdateFeeStructureRequest;
import com.rrtechnosoft.lms.dto.response.FeeStructureResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.FeeStructureService;
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

import java.util.UUID;

@RestController
@RequestMapping("/finance/fee-structures")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @GetMapping
    public ResponseEntity<Page<FeeStructureResponse>> list(@RequestParam(required = false) UUID courseId,
                                                             @RequestParam(defaultValue = "true") boolean activeOnly,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(feeStructureService.list(courseId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeStructureResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(feeStructureService.get(id));
    }

    @PostMapping
    public ResponseEntity<FeeStructureResponse> create(@Valid @RequestBody CreateFeeStructureRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeStructureService.create(request, principal.getId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FeeStructureResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateFeeStructureRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(feeStructureService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        feeStructureService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
