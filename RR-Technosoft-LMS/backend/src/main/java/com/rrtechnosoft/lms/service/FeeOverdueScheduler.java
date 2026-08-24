package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.StudentFee;
import com.rrtechnosoft.lms.entity.StudentFeeInstallment;
import com.rrtechnosoft.lms.entity.enums.InstallmentStatus;
import com.rrtechnosoft.lms.repository.StudentFeeInstallmentRepository;
import com.rrtechnosoft.lms.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Daily sweep that flags unpaid installments (and their parent StudentFee) as OVERDUE. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeOverdueScheduler {

    private final StudentFeeInstallmentRepository installmentRepository;
    private final StudentFeeRepository studentFeeRepository;

    @Scheduled(cron = "0 15 0 * * *") // 00:15 daily
    @Transactional
    public void markOverdue() {
        List<StudentFeeInstallment> overdue = installmentRepository.findOverdue(LocalDate.now());
        if (overdue.isEmpty()) return;

        Set<UUID> feeIds = new HashSet<>();
        for (StudentFeeInstallment installment : overdue) {
            installment.setStatus(InstallmentStatus.OVERDUE);
            feeIds.add(installment.getStudentFee().getId());
        }
        installmentRepository.saveAll(overdue);

        for (UUID feeId : feeIds) {
            StudentFee fee = studentFeeRepository.findWithDetailsById(feeId).orElse(null);
            if (fee == null) continue;
            StudentFeeService.recompute(fee);
            studentFeeRepository.save(fee);
        }
        log.info("Marked {} installments across {} fee records as OVERDUE", overdue.size(), feeIds.size());
    }
}
