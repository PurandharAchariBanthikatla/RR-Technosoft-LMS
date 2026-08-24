package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.StudentFeeInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StudentFeeInstallmentRepository extends JpaRepository<StudentFeeInstallment, UUID> {
    List<StudentFeeInstallment> findByStudentFeeIdOrderByInstallmentNumberAsc(UUID studentFeeId);

    @org.springframework.data.jpa.repository.Query("""
        select i from StudentFeeInstallment i
        where i.status in (com.rrtechnosoft.lms.entity.enums.InstallmentStatus.PENDING,
                            com.rrtechnosoft.lms.entity.enums.InstallmentStatus.PARTIAL)
          and i.dueDate < :today
        """)
    List<StudentFeeInstallment> findOverdue(@org.springframework.data.repository.query.Param("today") LocalDate today);
}
