package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.FeeStructureInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeStructureInstallmentRepository extends JpaRepository<FeeStructureInstallment, UUID> {
    List<FeeStructureInstallment> findByFeeStructureIdOrderByInstallmentNumberAsc(UUID feeStructureId);
    void deleteByFeeStructureId(UUID feeStructureId);
}
