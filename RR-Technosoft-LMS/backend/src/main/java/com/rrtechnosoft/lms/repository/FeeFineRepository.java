package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.FeeFine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeFineRepository extends JpaRepository<FeeFine, UUID> {
    List<FeeFine> findByStudentFeeIdOrderByCreatedAtDesc(UUID studentFeeId);
}
