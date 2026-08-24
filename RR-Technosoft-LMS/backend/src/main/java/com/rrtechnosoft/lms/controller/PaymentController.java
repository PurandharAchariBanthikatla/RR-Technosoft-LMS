package com.rrtechnosoft.lms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrtechnosoft.lms.dto.request.*;
import com.rrtechnosoft.lms.dto.response.PaymentOrderResponse;
import com.rrtechnosoft.lms.dto.response.PaymentResponse;
import com.rrtechnosoft.lms.dto.response.RefundResponse;
import com.rrtechnosoft.lms.entity.enums.PaymentStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/finance/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> list(@RequestParam(required = false) UUID studentId,
                                                        @RequestParam(required = false) UUID studentFeeId,
                                                        @RequestParam(required = false) PaymentStatus status,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentService.list(studentId, studentFeeId, status, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<PaymentResponse>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.listForStudent(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        boolean isStudent = principal.getUser().getRole() == UserRole.STUDENT;
        return ResponseEntity.ok(paymentService.get(id, principal.getId(), isStudent));
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentOrderResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiate(request, principal.getId()));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> verify(@Valid @RequestBody VerifyPaymentRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.verify(request, principal.getId()));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PaymentResponse> recordManual(@Valid @RequestBody RecordManualPaymentRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.recordManual(request, principal.getId()));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<RefundResponse> refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.refund(id, request, principal.getId()));
    }

    /**
     * Public webhook — no JWT (the gateway can't authenticate as a user), so
     * every event is verified via HMAC signature against the raw body before
     * anything is trusted. Kept open in SecurityConfig ("/finance/payments/webhook").
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        try {
            JsonNode json = objectMapper.readTree(rawBody);
            String eventType = json.path("event").asText(null);
            JsonNode paymentEntity = json.path("payload").path("payment").path("entity");
            String gatewayOrderId = paymentEntity.path("order_id").asText(null);
            String gatewayPaymentId = paymentEntity.path("id").asText(null);
            paymentService.handleWebhook(rawBody, signature, eventType, gatewayOrderId, gatewayPaymentId);
        } catch (Exception e) {
            log.error("Failed to process payment webhook", e);
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
