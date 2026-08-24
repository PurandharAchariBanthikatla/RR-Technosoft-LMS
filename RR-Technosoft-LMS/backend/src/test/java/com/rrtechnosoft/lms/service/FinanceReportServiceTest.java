package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.repository.StudentFeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceReportServiceTest {

    @Mock private StudentFeeRepository studentFeeRepository;

    @InjectMocks private FinanceReportService financeReportService;

    @Test
    void summary_outstandingIsBilledMinusCollected() {
        UUID courseId = UUID.randomUUID();
        when(studentFeeRepository.totalBilled(courseId)).thenReturn(new BigDecimal("10000"));
        when(studentFeeRepository.totalCollected(courseId)).thenReturn(new BigDecimal("6500"));
        when(studentFeeRepository.countTotal(courseId)).thenReturn(20L);
        when(studentFeeRepository.countOverdue(courseId)).thenReturn(3L);

        var response = financeReportService.summary(courseId);

        assertThat(response.totalOutstanding()).isEqualByComparingTo("3500");
        assertThat(response.totalStudentFees()).isEqualTo(20L);
        assertThat(response.overdueCount()).isEqualTo(3L);
    }

    @Test
    void summary_collectedExceedingBilled_clampsOutstandingAtZero() {
        // e.g. a fine was later waived after collection — shouldn't ever show negative outstanding
        UUID courseId = UUID.randomUUID();
        when(studentFeeRepository.totalBilled(courseId)).thenReturn(new BigDecimal("1000"));
        when(studentFeeRepository.totalCollected(courseId)).thenReturn(new BigDecimal("1200"));
        when(studentFeeRepository.countTotal(courseId)).thenReturn(5L);
        when(studentFeeRepository.countOverdue(courseId)).thenReturn(0L);

        var response = financeReportService.summary(courseId);

        assertThat(response.totalOutstanding()).isEqualByComparingTo("0");
    }

    @Test
    void summary_nullCourseId_isPassedThroughForAnOrgWideReport() {
        when(studentFeeRepository.totalBilled(null)).thenReturn(BigDecimal.ZERO);
        when(studentFeeRepository.totalCollected(null)).thenReturn(BigDecimal.ZERO);
        when(studentFeeRepository.countTotal(null)).thenReturn(0L);
        when(studentFeeRepository.countOverdue(null)).thenReturn(0L);

        var response = financeReportService.summary(null);

        assertThat(response.totalBilled()).isEqualByComparingTo("0");
    }
}
