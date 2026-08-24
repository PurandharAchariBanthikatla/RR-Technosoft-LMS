package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    List<PaymentRefund> findByPaymentIdOrderByRequestedAtDesc(UUID paymentId);

    @org.springframework.data.jpa.repository.Query("""
        select coalesce(sum(r.amount),0) from PaymentRefund r
        where r.payment.id = :paymentId and r.status = com.rrtechnosoft.lms.entity.enums.RefundStatus.PROCESSED
        """)
    BigDecimal totalProcessedForPayment(UUID paymentId);
}
