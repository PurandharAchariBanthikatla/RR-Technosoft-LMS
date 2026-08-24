package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateStudentRequest;
import com.rrtechnosoft.lms.dto.request.UpdateStudentRequest;
import com.rrtechnosoft.lms.dto.response.UserSummaryResponse;
import com.rrtechnosoft.lms.entity.enums.AccountStatus;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.StudentManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/students/manage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class StudentManagementController {

    private final StudentManagementService studentManagementService;

    @PostMapping
    public ResponseEntity<UserSummaryResponse> create(@Valid @RequestBody CreateStudentRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryResponse created = studentManagementService.createStudent(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> list(@RequestParam(required = false) String search,
                                                            Pageable pageable) {
        return ResponseEntity.ok(studentManagementService.listStudents(search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateStudentRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentManagementService.updateStudent(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> setStatus(@PathVariable UUID id,
                                                           @RequestParam AccountStatus status,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentManagementService.setStudentStatus(id, status, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        studentManagementService.deleteStudent(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
