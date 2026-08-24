package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByPaymentId(UUID paymentId);

    @EntityGraph(attributePaths = {"payment", "studentFee", "studentFee.student"})
    @Query("""
        select r from Receipt r
        where (:studentId is null or r.studentFee.student.id = :studentId)
        order by r.issuedAt desc
        """)
    Page<Receipt> search(@Param("studentId") UUID studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"payment", "studentFee", "studentFee.student"})
    List<Receipt> findByStudentFee_Student_IdOrderByIssuedAtDesc(UUID studentId);

    @EntityGraph(attributePaths = {"payment", "studentFee", "studentFee.student"})
    Optional<Receipt> findWithDetailsById(UUID id);
}
