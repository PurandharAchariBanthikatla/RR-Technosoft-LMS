package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.AssignFeeStructureRequest;
import com.rrtechnosoft.lms.dto.request.CreateDiscountRequest;
import com.rrtechnosoft.lms.dto.request.CreateFineRequest;
import com.rrtechnosoft.lms.dto.response.StudentFeeResponse;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.ReceiptService;
import com.rrtechnosoft.lms.service.StudentFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/finance/student-fees")
@RequiredArgsConstructor
public class StudentFeeController {

    private final StudentFeeService studentFeeService;
    private final ReceiptService receiptService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<StudentFeeResponse>> list(@RequestParam(required = false) UUID studentId,
                                                           @RequestParam(required = false) UUID courseId,
                                                           @RequestParam(required = false) FeeStatus status,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentFeeService.list(studentId, courseId, status, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentFeeResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentFeeService.listForStudent(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentFeeResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(studentFeeService.get(id, principal.getId(), isStudent));
    }

    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoice(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        var fee = studentFeeService.findWithDetails(id);
        if (isStudent && !fee.getStudent().getId().equals(principal.getId())) {
            throw ApiException.forbidden("You cannot view another student's invoice");
        }
        byte[] pdf = receiptService.buildInvoicePdf(fee);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<StudentFeeResponse> assign(@Valid @RequestBody AssignFeeStructureRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentFeeService.assign(request, principal.getId()));
    }

    @PostMapping("/{id}/discounts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<StudentFeeResponse> addDiscount(@PathVariable UUID id,
                                                            @Valid @RequestBody CreateDiscountRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentFeeService.addDiscount(id, request, principal.getId()));
    }

    @PostMapping("/{id}/fines")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<StudentFeeResponse> addFine(@PathVariable UUID id,
                                                        @Valid @RequestBody CreateFineRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentFeeService.addFine(id, request, principal.getId()));
    }

    @PatchMapping("/{id}/fines/{fineId}/waive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<StudentFeeResponse> waiveFine(@PathVariable UUID id, @PathVariable UUID fineId,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(studentFeeService.waiveFine(id, fineId, principal.getId()));
    }
}
