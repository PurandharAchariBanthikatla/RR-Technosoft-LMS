package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.reports.*;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import com.rrtechnosoft.lms.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private LiveClassRepository liveClassRepository;

    @InjectMocks private ReportsService reportsService;

    @Test
    void dashboardAnalytics_aggregatesAcrossAllModulesIntoOneResponse() {
        when(userRepository.countByRole(UserRole.STUDENT)).thenReturn(120L);
        when(courseRepository.count()).thenReturn(15L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE)).thenReturn(90L);
        when(enrollmentRepository.sumRevenueByStatuses(anyList())).thenReturn(BigDecimal.valueOf(450000));
        when(liveClassRepository.countUpcomingBetween(any(), any())).thenReturn(4L);
        when(submissionRepository.countPendingGrading()).thenReturn(7L);
        when(courseRepository.countDistinctInstructors()).thenReturn(6L);
        when(attendanceRepository.overallAttendancePercentage()).thenReturn(87.5);
        when(enrollmentRepository.overallAvgProgress()).thenReturn(62.3);
        when(enrollmentRepository.monthlyEnrollmentCounts(any())).thenReturn(List.of());
        when(enrollmentRepository.monthlyRevenue(any())).thenReturn(List.of());
        when(attendanceRepository.monthlyAttendancePercentage(any())).thenReturn(List.of());
        when(courseRepository.courseDistributionRaw()).thenReturn(List.of());

        DashboardAnalyticsResponse result = reportsService.dashboardAnalytics();

        assertThat(result.totalStudents()).isEqualTo(120L);
        assertThat(result.totalCourses()).isEqualTo(15L);
        assertThat(result.activeEnrollments()).isEqualTo(90L);
        assertThat(result.totalRevenue()).isEqualByComparingTo("450000");
        assertThat(result.upcomingLiveClasses()).isEqualTo(4L);
        assertThat(result.pendingAssignments()).isEqualTo(7L);
        assertThat(result.totalFaculty()).isEqualTo(6L);
        assertThat(result.avgAttendancePercentage()).isEqualTo(87.5);
        // 450000 revenue / 120 students
        assertThat(result.averageRevenuePerStudent()).isEqualByComparingTo("3750.00");
    }

    @Test
    void dashboardAnalytics_averageRevenuePerStudent_isZero_whenNoStudents() {
        when(userRepository.countByRole(UserRole.STUDENT)).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(enrollmentRepository.countByStatus(any())).thenReturn(0L);
        when(enrollmentRepository.sumRevenueByStatuses(anyList())).thenReturn(BigDecimal.ZERO);
        when(liveClassRepository.countUpcomingBetween(any(), any())).thenReturn(0L);
        when(submissionRepository.countPendingGrading()).thenReturn(0L);
        when(courseRepository.countDistinctInstructors()).thenReturn(0L);
        when(attendanceRepository.overallAttendancePercentage()).thenReturn(0.0);
        when(enrollmentRepository.overallAvgProgress()).thenReturn(0.0);
        when(enrollmentRepository.monthlyEnrollmentCounts(any())).thenReturn(List.of());
        when(enrollmentRepository.monthlyRevenue(any())).thenReturn(List.of());
        when(attendanceRepository.monthlyAttendancePercentage(any())).thenReturn(List.of());
        when(courseRepository.courseDistributionRaw()).thenReturn(List.of());

        DashboardAnalyticsResponse result = reportsService.dashboardAnalytics();

        assertThat(result.averageRevenuePerStudent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void attendanceReport_computesPercentageFromStatusCounts() {
        UUID courseId = UUID.randomUUID();
        AttendanceReportAggProjection row = new AttendanceReportAggProjection() {
            public UUID getCourseId() { return courseId; }
            public String getCourseTitle() { return "Java Fundamentals"; }
            public Long getSessionsHeld() { return 10L; }
            public Long getPresentCount() { return 80L; }
            public Long getAbsentCount() { return 15L; }
            public Long getLateCount() { return 4L; }
            public Long getExcusedCount() { return 1L; }
        };
        Pageable pageable = PageRequest.of(0, 20);
        Page<AttendanceReportAggProjection> page = new PageImpl<>(List.of(row), pageable, 1);
        when(attendanceRepository.attendanceByCourse(any(), any(), any(), any())).thenReturn(page);

        Page<AttendanceReportRowResponse> result = reportsService.attendanceReport(courseId,
                LocalDate.now().minusMonths(1), LocalDate.now(), pageable);

        assertThat(result.getContent()).hasSize(1);
        AttendanceReportRowResponse r = result.getContent().get(0);
        assertThat(r.presentCount()).isEqualTo(80L);
        // 80 / (80+15+4+1) * 100 = 80.0
        assertThat(r.attendancePercentage()).isEqualTo(80.0);
    }

    @Test
    void revenueReport_multipliesUnitPriceByPaidEnrollments() {
        UUID courseId = UUID.randomUUID();
        EnrollmentRepository.RevenueRowNative row = new EnrollmentRepository.RevenueRowNative() {
            public UUID getCourseId() { return courseId; }
            public String getCourseTitle() { return "Full Stack Java"; }
            public String getCategory() { return "Programming"; }
            public BigDecimal getUnitPrice() { return BigDecimal.valueOf(25000); }
            public Long getPaidEnrollments() { return 12L; }
            public Long getDroppedOrPendingEnrollments() { return 3L; }
        };
        Pageable pageable = PageRequest.of(0, 20);
        when(enrollmentRepository.revenueByCourse(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 1));

        Page<RevenueReportRowResponse> result = reportsService.revenueReport(courseId,
                OffsetDateTime.now().minusMonths(1), OffsetDateTime.now(), pageable);

        assertThat(result.getContent()).hasSize(1);
        RevenueReportRowResponse r = result.getContent().get(0);
        assertThat(r.totalRevenue()).isEqualByComparingTo("300000");
    }
}
