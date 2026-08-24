package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.response.ReceiptResponse;
import com.rrtechnosoft.lms.entity.Receipt;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.ReceiptRepository;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/finance/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<ReceiptResponse>> list(@RequestParam(required = false) UUID studentId,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(receiptRepository.search(studentId, pageable).map(ReceiptResponse::from));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ReceiptResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(receiptRepository.findByStudentFee_Student_IdOrderByIssuedAtDesc(principal.getId())
                .stream().map(ReceiptResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        Receipt receipt = authorize(id, principal);
        return ResponseEntity.ok(ReceiptResponse.from(receipt));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        Receipt receipt = authorize(id, principal);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=" + receipt.getReceiptNumber() + ".pdf")
                .body(receipt.getPdfData());
    }

    private Receipt authorize(UUID id, UserPrincipal principal) {
        Receipt receipt = receiptService.getWithDetails(id);
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        if (isStudent && !receipt.getStudentFee().getStudent().getId().equals(principal.getId())) {
            throw ApiException.forbidden("You cannot view another student's receipt");
        }
        return receipt;
    }
}
