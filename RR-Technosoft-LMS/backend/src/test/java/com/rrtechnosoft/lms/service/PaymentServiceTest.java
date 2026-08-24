package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.InitiatePaymentRequest;
import com.rrtechnosoft.lms.dto.request.RecordManualPaymentRequest;
import com.rrtechnosoft.lms.dto.request.RefundRequest;
import com.rrtechnosoft.lms.dto.request.VerifyPaymentRequest;
import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.*;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.payment.PaymentGatewayService;
import com.rrtechnosoft.lms.repository.PaymentRefundRepository;
import com.rrtechnosoft.lms.repository.PaymentRepository;
import com.rrtechnosoft.lms.repository.StudentFeeInstallmentRepository;
import com.rrtechnosoft.lms.repository.StudentFeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentRefundRepository refundRepository;
    @Mock private StudentFeeRepository studentFeeRepository;
    @Mock private StudentFeeInstallmentRepository installmentRepository;
    @Mock private PaymentGatewayService gatewayService;
    @Mock private ReceiptService receiptService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private PaymentService paymentService;

    private final UUID studentId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    // ------------------------------------------------------------ initiate

    @Test
    void initiate_rejectsAmountAboveOutstandingBalance() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        InitiatePaymentRequest request = new InitiatePaymentRequest(feeId, null, new BigDecimal("2000"));

        assertThatThrownBy(() -> paymentService.initiate(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds the outstanding balance");
    }

    @Test
    void initiate_rejectsZeroOrNegativeAmount() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        InitiatePaymentRequest request = new InitiatePaymentRequest(feeId, null, BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentService.initiate(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void initiate_rejectsPayingAnotherStudentsFee() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(UUID.randomUUID(), new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        InitiatePaymentRequest request = new InitiatePaymentRequest(feeId, null, new BigDecimal("100"));

        assertThatThrownBy(() -> paymentService.initiate(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot pay another student's fee");
    }

    @Test
    void initiate_withinBalance_createsGatewayOrderAndReturnsIt() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(gatewayService.createOrder(any(), anyString(), anyString()))
                .thenReturn(new PaymentGatewayService.GatewayOrder("order_123", "INR", 50000, "key_test"));

        var response = paymentService.initiate(new InitiatePaymentRequest(feeId, null, new BigDecimal("500")), studentId);

        assertThat(response.gatewayOrderId()).isEqualTo("order_123");
        assertThat(response.amount()).isEqualByComparingTo("500");
        verify(auditLogService).log(studentId, "INITIATE_PAYMENT", "Payment", response.paymentId(), null);
    }

    // -------------------------------------------------------------- verify

    @Test
    void verify_wrongStatus_throwsConflict() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = paymentOwnedBy(studentId, new BigDecimal("500"));
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));

        VerifyPaymentRequest request = new VerifyPaymentRequest(paymentId, "order_1", "pay_1", "sig_1");

        assertThatThrownBy(() -> paymentService.verify(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not awaiting verification");
    }

    @Test
    void verify_orderIdMismatch_throwsBadRequest() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = paymentOwnedBy(studentId, new BigDecimal("500"));
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayOrderId("order_real");
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));

        VerifyPaymentRequest request = new VerifyPaymentRequest(paymentId, "order_spoofed", "pay_1", "sig_1");

        assertThatThrownBy(() -> paymentService.verify(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Order id does not match");
    }

    @Test
    void verify_invalidSignature_marksFailedAndThrows() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = paymentOwnedBy(studentId, new BigDecimal("500"));
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayOrderId("order_1");
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));
        when(gatewayService.verifyPaymentSignature("order_1", "pay_1", "bad_sig")).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyPaymentRequest request = new VerifyPaymentRequest(paymentId, "order_1", "pay_1", "bad_sig");

        assertThatThrownBy(() -> paymentService.verify(request, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Payment verification failed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void verify_validSignature_appliesToLedgerAndGeneratesReceipt() {
        UUID paymentId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        Payment payment = Payment.builder().id(paymentId).student(fee.getStudent()).studentFee(fee)
                .amount(new BigDecimal("500")).currency("INR").status(PaymentStatus.PENDING)
                .gatewayOrderId("order_1").build();
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));
        when(gatewayService.verifyPaymentSignature("order_1", "pay_1", "good_sig")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyPaymentRequest request = new VerifyPaymentRequest(paymentId, "order_1", "pay_1", "good_sig");
        var response = paymentService.verify(request, studentId);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(fee.getAmountPaid()).isEqualByComparingTo("500");
        verify(receiptService).generateForPayment(payment, null);
    }

    // -------------------------------------------------------- recordManual

    @Test
    void recordManual_rejectsAmountAboveOutstandingBalance() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));

        RecordManualPaymentRequest request = new RecordManualPaymentRequest(feeId, null, new BigDecimal("5000"), PaymentMethod.CASH, null);

        assertThatThrownBy(() -> paymentService.recordManual(request, adminId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds the outstanding balance");
    }

    @Test
    void recordManual_marksSuccessImmediatelyAndUpdatesLedger() {
        UUID feeId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), BigDecimal.ZERO);
        fee.setId(feeId);
        when(studentFeeRepository.findWithDetailsById(feeId)).thenReturn(Optional.of(fee));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordManualPaymentRequest request = new RecordManualPaymentRequest(feeId, null, new BigDecimal("500"), PaymentMethod.CASH, "front desk");
        var response = paymentService.recordManual(request, adminId);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(fee.getAmountPaid()).isEqualByComparingTo("500");
        verify(receiptService).generateForPayment(any(Payment.class), eq(adminId));
    }

    // -------------------------------------------------------------- refund

    @Test
    void refund_onlySuccessfulOrPartiallyRefundedPaymentCanBeRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = paymentOwnedBy(studentId, new BigDecimal("500"));
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(paymentId, new RefundRequest(new BigDecimal("100"), "not happy"), adminId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only a successful payment can be refunded");
    }

    @Test
    void refund_rejectsAmountAboveRefundableBalance() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = paymentOwnedBy(studentId, new BigDecimal("500"));
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRefundedAmount(new BigDecimal("400"));
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(paymentId, new RefundRequest(new BigDecimal("200"), "too much"), adminId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds refundable balance");
    }

    @Test
    void refund_fullAmount_marksPaymentFullyRefundedAndReversesLedger() {
        UUID paymentId = UUID.randomUUID();
        StudentFee fee = feeOwnedBy(studentId, new BigDecimal("1000"), new BigDecimal("500"));
        StudentFeeInstallment installment = fee.getInstallments().get(0);
        installment.setPaidAmount(new BigDecimal("500"));
        installment.setStatus(InstallmentStatus.PARTIAL);

        Payment payment = Payment.builder().id(paymentId).student(fee.getStudent()).studentFee(fee)
                .installment(installment).amount(new BigDecimal("500")).status(PaymentStatus.SUCCESS)
                .gatewayProvider(PaymentGatewayProvider.MANUAL).refundedAmount(BigDecimal.ZERO).build();
        when(paymentRepository.findWithDetailsById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(PaymentRefund.class))).thenAnswer(inv -> {
            PaymentRefund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentFeeRepository.save(any(StudentFee.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = paymentService.refund(paymentId, new RefundRequest(new BigDecimal("500"), "student withdrew"), adminId);

        assertThat(response.status()).isEqualTo(RefundStatus.PROCESSED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(fee.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(installment.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PENDING);
    }

    // ------------------------------------------------------------ helpers

    private StudentFee feeOwnedBy(UUID owner, BigDecimal totalAmount, BigDecimal amountPaid) {
        StudentFee fee = StudentFee.builder()
                .student(User.builder().id(owner).build())
                .totalAmount(totalAmount)
                .netPayable(totalAmount)
                .amountPaid(amountPaid)
                .currency("INR")
                .status(FeeStatus.PENDING)
                .build();
        fee.getInstallments().add(StudentFeeInstallment.builder()
                .studentFee(fee)
                .installmentNumber(1)
                .amount(totalAmount)
                .paidAmount(amountPaid)
                .dueDate(LocalDate.now().plusDays(10))
                .status(InstallmentStatus.PENDING)
                .build());
        return fee;
    }

    private Payment paymentOwnedBy(UUID owner, BigDecimal amount) {
        return Payment.builder()
                .student(User.builder().id(owner).build())
                .amount(amount)
                .currency("INR")
                .status(PaymentStatus.INITIATED)
                .refundedAmount(BigDecimal.ZERO)
                .build();
    }
}
