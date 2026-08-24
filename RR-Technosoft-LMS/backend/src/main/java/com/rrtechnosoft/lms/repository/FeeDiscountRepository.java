package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.FeeDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeDiscountRepository extends JpaRepository<FeeDiscount, UUID> {
    List<FeeDiscount> findByStudentFeeIdOrderByCreatedAtDesc(UUID studentFeeId);
}
