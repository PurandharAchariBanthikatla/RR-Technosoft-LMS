package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.Payment;
import com.rrtechnosoft.lms.entity.Receipt;
import com.rrtechnosoft.lms.entity.StudentFee;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import com.rrtechnosoft.lms.entity.enums.PaymentGatewayProvider;
import com.rrtechnosoft.lms.entity.enums.PaymentMethod;
import com.rrtechnosoft.lms.entity.enums.PaymentStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.ReceiptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private ReceiptService receiptService;

    @Test
    void generateForPayment_mintsReceiptNumberFromSequenceAndEmbedsAPdf() {
        when(jdbcTemplate.queryForObject(eq("select nextval('receipt_number_seq')"), eq(Long.class))).thenReturn(42L);
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentFee fee = StudentFee.builder()
                .student(User.builder().id(UUID.randomUUID()).fullName("Asha Rao").studentId("RRT-001").build())
                .course(Course.builder().title("DevOps Foundations").build())
                .totalAmount(new BigDecimal("1000")).discountAmount(BigDecimal.ZERO).fineAmount(BigDecimal.ZERO)
                .netPayable(new BigDecimal("1000")).amountPaid(new BigDecimal("500"))
                .currency("INR").status(FeeStatus.PARTIAL).build();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID()).student(fee.getStudent()).studentFee(fee)
                .amount(new BigDecimal("500")).currency("INR").method(PaymentMethod.UPI)
                .gatewayProvider(PaymentGatewayProvider.RAZORPAY).status(PaymentStatus.SUCCESS)
                .paidAt(OffsetDateTime.now()).build();

        Receipt receipt = receiptService.generateForPayment(payment, null);

        String expectedYear = String.valueOf(Year.now().getValue());
        assertThat(receipt.getReceiptNumber()).isEqualTo("RRT-RCPT-" + expectedYear + "-000042");
        assertThat(receipt.getAmount()).isEqualByComparingTo("500");
        assertThat(receipt.getPdfData()).isNotEmpty();
        // A real PDF starts with the "%PDF-" magic bytes.
        assertThat(new String(receipt.getPdfData(), 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void buildInvoicePdf_producesNonEmptyPdfBytes() {
        StudentFee fee = StudentFee.builder()
                .student(User.builder().id(UUID.randomUUID()).fullName("Asha Rao").studentId("RRT-001").build())
                .totalAmount(new BigDecimal("1000")).discountAmount(BigDecimal.ZERO).fineAmount(BigDecimal.ZERO)
                .netPayable(new BigDecimal("1000")).amountPaid(BigDecimal.ZERO)
                .currency("INR").status(FeeStatus.PENDING).build();

        byte[] pdf = receiptService.buildInvoicePdf(fee);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void getWithDetails_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(receiptRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.getWithDetails(id))
                .isInstanceOf(ApiException.class)
                .hasMessage("Receipt not found");
    }
}
