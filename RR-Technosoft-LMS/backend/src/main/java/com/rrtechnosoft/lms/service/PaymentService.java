package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.*;
import com.rrtechnosoft.lms.dto.response.PaymentOrderResponse;
import com.rrtechnosoft.lms.dto.response.PaymentResponse;
import com.rrtechnosoft.lms.dto.response.RefundResponse;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.InstallmentStatus;
import com.rrtechnosoft.lms.entity.enums.PaymentStatus;
import com.rrtechnosoft.lms.entity.enums.RefundStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.payment.PaymentGatewayService;
import com.rrtechnosoft.lms.repository.PaymentRefundRepository;
import com.rrtechnosoft.lms.repository.PaymentRepository;
import com.rrtechnosoft.lms.repository.StudentFeeInstallmentRepository;
import com.rrtechnosoft.lms.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Owns the full payment lifecycle: gateway order creation, client-side
 * signature verification, async webhook confirmation, manual/offline
 * recording, and refunds. Every state transition that lands a payment in
 * SUCCESS applies the amount to the installment/StudentFee ledger and
 * mints a receipt in the same transaction, so the ledger is never out of
 * sync with a "successful" payment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final StudentFeeInstallmentRepository installmentRepository;
    private final PaymentGatewayService gatewayService;
    private final ReceiptService receiptService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(UUID studentId, UUID studentFeeId, PaymentStatus status, Pageable pageable) {
        return paymentRepository.search(studentId, studentFeeId, status, pageable).map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForStudent(UUID studentId) {
        return paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID id, UUID viewerId, boolean isStudent) {
        Payment payment = findWithDetails(id);
        if (isStudent && !payment.getStudent().getId().equals(viewerId)) {
            throw ApiException.forbidden("You cannot view another student's payment");
        }
        return PaymentResponse.from(payment);
    }

    private Payment findWithDetails(UUID id) {
        return paymentRepository.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Payment not found"));
    }

    // ---------------------------------------------------------------
    // Online (gateway) flow
    // ---------------------------------------------------------------

    @Transactional
    public PaymentOrderResponse initiate(InitiatePaymentRequest request, UUID studentId) {
        StudentFee fee = studentFeeRepository.findWithDetailsById(request.studentFeeId())
                .orElseThrow(() -> ApiException.notFound("Student fee record not found"));
        if (!fee.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("You cannot pay another student's fee");
        }

        StudentFeeInstallment installment = null;
        BigDecimal maxPayable;
        if (request.installmentId() != null) {
            installment = fee.getInstallments().stream()
                    .filter(i -> i.getId().equals(request.installmentId())).findFirst()
                    .orElseThrow(() -> ApiException.notFound("Installment not found"));
            maxPayable = installment.getBalanceDue();
        } else {
            maxPayable = fee.getBalanceDue();
        }
        if (request.amount().compareTo(maxPayable) > 0) {
            throw ApiException.badRequest("Amount exceeds the outstanding balance of " + maxPayable);
        }
        if (request.amount().signum() <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero");
        }

        guardAgainstDuplicateInFlightPayment(fee.getId(), request.installmentId());

        Payment payment = Payment.builder()
                .studentFee(fee)
                .installment(installment)
                .student(fee.getStudent())
                .amount(request.amount())
                .currency(fee.getCurrency())
                .status(PaymentStatus.INITIATED)
                .build();
        payment = paymentRepository.save(payment);

        PaymentGatewayService.GatewayOrder order = gatewayService.createOrder(
                request.amount(), fee.getCurrency(), payment.getId().toString());
        payment.setGatewayOrderId(order.orderId());
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        auditLogService.log(studentId, "INITIATE_PAYMENT", "Payment", payment.getId(), null);
        return new PaymentOrderResponse(payment.getId(), order.orderId(), request.amount(), fee.getCurrency(),
                order.keyId(), payment.getGatewayProvider().name());
    }

    /**
     * Duplicate-payment protection: refuses to open a second gateway order
     * while an earlier attempt for the same fee/installment is still
     * in-flight (INITIATED/PENDING). Stale attempts (older than
     * {@link #IN_FLIGHT_EXPIRY_MINUTES}) are treated as abandoned, marked
     * FAILED, and no longer block a new attempt — this is what lets a
     * student retry after e.g. closing the Razorpay modal without paying.
     */
    private static final long IN_FLIGHT_EXPIRY_MINUTES = 15;

    private void guardAgainstDuplicateInFlightPayment(UUID studentFeeId, UUID installmentId) {
        List<Payment> inFlight = paymentRepository.findInFlight(studentFeeId, installmentId);
        for (Payment existing : inFlight) {
            boolean expired = existing.getCreatedAt() != null
                    && existing.getCreatedAt().isBefore(OffsetDateTime.now().minusMinutes(IN_FLIGHT_EXPIRY_MINUTES));
            if (expired) {
                existing.setStatus(PaymentStatus.FAILED);
                existing.setFailureReason("Abandoned — superseded by a new payment attempt");
                paymentRepository.save(existing);
            } else {
                throw ApiException.conflict(
                        "A payment for this fee is already in progress. Please complete or wait a few minutes before retrying.");
            }
        }
    }

    @Transactional
    public PaymentResponse verify(VerifyPaymentRequest request, UUID studentId) {
        Payment payment = findWithDetails(request.paymentId());
        if (!payment.getStudent().getId().equals(studentId)) {
            throw ApiException.forbidden("You cannot verify another student's payment");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.conflict("Payment is not awaiting verification (status=" + payment.getStatus() + ")");
        }
        if (!request.gatewayOrderId().equals(payment.getGatewayOrderId())) {
            throw ApiException.badRequest("Order id does not match this payment");
        }

        boolean valid = gatewayService.verifyPaymentSignature(
                request.gatewayOrderId(), request.gatewayPaymentId(), request.gatewaySignature());
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            auditLogService.log(studentId, "PAYMENT_VERIFICATION_FAILED", "Payment", payment.getId(), null);
            throw ApiException.badRequest("Payment verification failed. If money was deducted, it will be reconciled automatically.");
        }

        payment.setGatewayPaymentId(request.gatewayPaymentId());
        payment.setGatewaySignature(request.gatewaySignature());
        markSuccess(payment, studentId);
        return PaymentResponse.from(payment);
    }

    /**
     * Async confirmation from the gateway (payment.captured / payment.failed / refund.processed).
     * Idempotent: re-deliveries of the same event are safe no-ops once a payment is already SUCCESS.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signatureHeader, String eventType,
                               String gatewayOrderId, String gatewayPaymentId) {
        if (!gatewayService.verifyWebhookSignature(rawBody, signatureHeader)) {
            throw ApiException.unauthorized("Invalid webhook signature");
        }
        var paymentOpt = gatewayPaymentId != null
                ? paymentRepository.findByGatewayPaymentId(gatewayPaymentId)
                : paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isEmpty()) {
            log.warn("Webhook for unknown payment: order={} payment={} event={}", gatewayOrderId, gatewayPaymentId, eventType);
            return;
        }
        Payment payment = paymentOpt.get();

        if ("payment.captured".equals(eventType)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setGatewayPaymentId(gatewayPaymentId);
                markSuccess(payment, null);
            }
        } else if ("payment.failed".equals(eventType)) {
            if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.INITIATED) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Gateway reported failure");
                paymentRepository.save(payment);
            }
        }
    }

    private void markSuccess(Payment payment, UUID actorId) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(OffsetDateTime.now());
        payment = paymentRepository.save(payment);

        applyPaymentToLedger(payment);
        receiptService.generateForPayment(payment, actorId);
        auditLogService.log(actorId != null ? actorId : payment.getStudent().getId(),
                "PAYMENT_SUCCESS", "Payment", payment.getId(), null);
    }

    private void applyPaymentToLedger(Payment payment) {
        StudentFee fee = payment.getStudentFee();
        fee.setAmountPaid(fee.getAmountPaid().add(payment.getAmount()));

        if (payment.getInstallment() != null) {
            applyToInstallment(payment.getInstallment(), payment.getAmount());
        } else {
            BigDecimal remaining = payment.getAmount();
            for (StudentFeeInstallment installment : fee.getInstallments()) {
                if (remaining.signum() <= 0) break;
                BigDecimal due = installment.getBalanceDue();
                if (due.signum() <= 0) continue;
                BigDecimal apply = due.min(remaining);
                applyToInstallment(installment, apply);
                remaining = remaining.subtract(apply);
            }
        }
        StudentFeeService.recompute(fee);
        studentFeeRepository.save(fee);
    }

    private void applyToInstallment(StudentFeeInstallment installment, BigDecimal amount) {
        installment.setPaidAmount(installment.getPaidAmount().add(amount));
        installment.setPaidAt(OffsetDateTime.now());
        installment.setStatus(installment.getBalanceDue().signum() <= 0 ? InstallmentStatus.PAID : InstallmentStatus.PARTIAL);
    }

    // ---------------------------------------------------------------
    // Manual / offline payments (admin-recorded cash, bank transfer, cheque)
    // ---------------------------------------------------------------

    @Transactional
    public PaymentResponse recordManual(RecordManualPaymentRequest request, UUID adminId) {
        StudentFee fee = studentFeeRepository.findWithDetailsById(request.studentFeeId())
                .orElseThrow(() -> ApiException.notFound("Student fee record not found"));
        StudentFeeInstallment installment = null;
        BigDecimal maxPayable = fee.getBalanceDue();
        if (request.installmentId() != null) {
            installment = fee.getInstallments().stream()
                    .filter(i -> i.getId().equals(request.installmentId())).findFirst()
                    .orElseThrow(() -> ApiException.notFound("Installment not found"));
            maxPayable = installment.getBalanceDue();
        }
        if (request.amount().compareTo(maxPayable) > 0) {
            throw ApiException.badRequest("Amount exceeds the outstanding balance of " + maxPayable);
        }

        Payment payment = Payment.builder()
                .studentFee(fee)
                .installment(installment)
                .student(fee.getStudent())
                .amount(request.amount())
                .currency(fee.getCurrency())
                .method(request.method())
                .gatewayProvider(com.rrtechnosoft.lms.entity.enums.PaymentGatewayProvider.MANUAL)
                .status(PaymentStatus.SUCCESS)
                .paidAt(OffsetDateTime.now())
                .recordedBy(adminId)
                .build();
        payment = paymentRepository.save(payment);

        applyPaymentToLedger(payment);
        receiptService.generateForPayment(payment, adminId);
        auditLogService.log(adminId, "RECORD_MANUAL_PAYMENT", "Payment", payment.getId(), null);
        return PaymentResponse.from(payment);
    }

    // ---------------------------------------------------------------
    // Refunds
    // ---------------------------------------------------------------

    @Transactional
    public RefundResponse refund(UUID paymentId, RefundRequest request, UUID adminId) {
        Payment payment = findWithDetails(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw ApiException.conflict("Only a successful payment can be refunded");
        }
        BigDecimal refundable = payment.getAmount().subtract(payment.getRefundedAmount());
        if (request.amount().compareTo(refundable) > 0) {
            throw ApiException.badRequest("Refund amount exceeds refundable balance of " + refundable);
        }

        PaymentRefund refund = PaymentRefund.builder()
                .payment(payment)
                .amount(request.amount())
                .reason(request.reason())
                .requestedBy(adminId)
                .build();
        refund = refundRepository.save(refund);

        if (payment.getGatewayProvider() == com.rrtechnosoft.lms.entity.enums.PaymentGatewayProvider.RAZORPAY
                && payment.getGatewayPaymentId() != null) {
            PaymentGatewayService.GatewayRefund gatewayRefund =
                    gatewayService.refund(payment.getGatewayPaymentId(), request.amount(), request.reason());
            refund.setGatewayRefundId(gatewayRefund.refundId());
        }
        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedBy(adminId);
        refund.setProcessedAt(OffsetDateTime.now());
        refund = refundRepository.save(refund);

        payment.setRefundedAmount(payment.getRefundedAmount().add(request.amount()));
        payment.setStatus(payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        reverseFromLedger(payment, request.amount());
        auditLogService.log(adminId, "ISSUE_REFUND", "PaymentRefund", refund.getId(), null);
        return RefundResponse.from(refund);
    }

    private void reverseFromLedger(Payment payment, BigDecimal amount) {
        StudentFee fee = payment.getStudentFee();
        fee.setAmountPaid(fee.getAmountPaid().subtract(amount).max(BigDecimal.ZERO));

        if (payment.getInstallment() != null) {
            reverseInstallment(payment.getInstallment(), amount);
        } else {
            BigDecimal remaining = amount;
            List<StudentFeeInstallment> paidDesc = fee.getInstallments().stream()
                    .filter(i -> i.getPaidAmount().signum() > 0)
                    .sorted((a, b) -> b.getInstallmentNumber() - a.getInstallmentNumber())
                    .toList();
            for (StudentFeeInstallment installment : paidDesc) {
                if (remaining.signum() <= 0) break;
                BigDecimal reduceBy = installment.getPaidAmount().min(remaining);
                reverseInstallment(installment, reduceBy);
                remaining = remaining.subtract(reduceBy);
            }
        }
        StudentFeeService.recompute(fee);
        studentFeeRepository.save(fee);
    }

    private void reverseInstallment(StudentFeeInstallment installment, BigDecimal amount) {
        installment.setPaidAmount(installment.getPaidAmount().subtract(amount).max(BigDecimal.ZERO));
        installment.setStatus(installment.getPaidAmount().signum() <= 0
                ? InstallmentStatus.PENDING
                : (installment.getBalanceDue().signum() <= 0 ? InstallmentStatus.PAID : InstallmentStatus.PARTIAL));
    }
}
