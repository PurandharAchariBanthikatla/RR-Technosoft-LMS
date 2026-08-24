package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.reports.*;
import com.rrtechnosoft.lms.entity.Assignment;
import com.rrtechnosoft.lms.entity.StudentProfile;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import com.rrtechnosoft.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reports & Analytics module.
 * <p>
 * Every report here is read-only and derived entirely from data the other
 * modules already own (Enrollments, Attendance, Assignments, Courses/Users) —
 * there is no separate "reporting" table. Two consequences worth calling out:
 * <ul>
 *   <li><b>Revenue</b> is computed from paid enrollments (status ACTIVE or
 *   COMPLETED) x {@code Course.price}. The schema has no payments/invoices
 *   table, so an enrollment in a paid state is the closest thing to a
 *   "transaction" it can report on.</li>
 *   <li><b>Faculty</b> is grouped by {@code Course.instructorName} (a free-text
 *   column already on Course). There is no Faculty entity or FACULTY role —
 *   {@code Course.instructorId} is reserved but unused (see Course.java) — so
 *   "faculty" in this module means the distinct instructor names on courses.</li>
 * </ul>
 * Every list method here follows the same batched-lookup shape used
 * elsewhere in the codebase (e.g. CourseService, AssignmentService): fetch a
 * page of "row subjects" with one query, then fetch aggregates for that
 * page's ids with a small, fixed number of follow-up queries — never one
 * query per row.
 */
@Service
@RequiredArgsConstructor
public class ReportsService {

    private static final List<EnrollmentStatus> PAID_STATUSES = List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED);

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final LiveClassRepository liveClassRepository;

    // =====================================================================
    // Dashboard Analytics
    // =====================================================================

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse dashboardAnalytics() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sixMonthsAgo = now.minusMonths(6);
        LocalDate sixMonthsAgoDate = LocalDate.now().minusMonths(6);

        long totalStudents = userRepository.countByRole(com.rrtechnosoft.lms.entity.enums.UserRole.STUDENT);
        long totalCourses = courseRepository.count();
        long activeEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);
        BigDecimal totalRevenue = enrollmentRepository.sumRevenueByStatuses(PAID_STATUSES);
        long upcomingLiveClasses = liveClassRepository.countUpcomingBetween(now, now.plusDays(7));
        long pendingAssignments = submissionRepository.countPendingGrading();
        long totalFaculty = courseRepository.countDistinctInstructors();
        double avgAttendance = attendanceRepository.overallAttendancePercentage();
        double avgCompletion = round2(enrollmentRepository.overallAvgProgress());
        BigDecimal avgRevenuePerStudent = totalStudents == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalStudents), 2, RoundingMode.HALF_UP);

        List<TrendPointResponse> studentGrowth = enrollmentRepository.monthlyEnrollmentCounts(sixMonthsAgo).stream()
                .map(r -> TrendPointResponse.of(r.getMonthLabel(), r.getVal()))
                .toList();
        List<TrendPointResponse> revenueTrend = enrollmentRepository.monthlyRevenue(sixMonthsAgo).stream()
                .map(r -> TrendPointResponse.of(r.getMonthLabel(), r.getVal()))
                .toList();
        List<TrendPointResponse> attendanceTrend = attendanceRepository.monthlyAttendancePercentage(sixMonthsAgoDate).stream()
                .map(r -> TrendPointResponse.of(r.getMonthLabel(), r.getVal()))
                .toList();
        List<CourseDistributionResponse> courseDistribution = courseRepository.courseDistributionRaw();

        return new DashboardAnalyticsResponse(
                totalStudents, totalCourses, activeEnrollments, totalRevenue,
                upcomingLiveClasses, pendingAssignments, studentGrowth,
                totalFaculty, avgAttendance, avgCompletion, avgRevenuePerStudent,
                revenueTrend, attendanceTrend, courseDistribution
        );
    }

    /**
     * Backs GET /dashboard/admin — the pre-existing admin dashboard widget
     * (src/app/(admin)/admin/dashboard/page.tsx), which was wired to this
     * endpoint in the frontend before any backend controller for it existed.
     * Kept separate from {@link #dashboardAnalytics()} so that contract never
     * has to change shape as the richer Reports & Analytics dashboard evolves.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboardStats() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sixMonthsAgo = now.minusMonths(6);

        long totalStudents = userRepository.countByRole(com.rrtechnosoft.lms.entity.enums.UserRole.STUDENT);
        long totalCourses = courseRepository.count();
        long activeEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);
        BigDecimal totalRevenue = enrollmentRepository.sumRevenueByStatuses(PAID_STATUSES);
        long upcomingLiveClasses = liveClassRepository.countUpcomingBetween(now, now.plusDays(7));
        long pendingAssignments = submissionRepository.countPendingGrading();

        List<AdminDashboardResponse.MonthlyStudents> studentGrowth = enrollmentRepository.monthlyEnrollmentCounts(sixMonthsAgo)
                .stream()
                .map(r -> new AdminDashboardResponse.MonthlyStudents(r.getMonthLabel(), r.getVal().longValue()))
                .toList();

        return new AdminDashboardResponse(totalStudents, totalCourses, activeEnrollments, totalRevenue,
                upcomingLiveClasses, pendingAssignments, studentGrowth);
    }

    // =====================================================================
    // Student Report
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<StudentReportRowResponse> studentReport(String search, String batch, String branch,
                                                          UUID courseId, Pageable pageable) {
        Page<User> page = userRepository.searchStudentsForReport(blankToNull(search), blankToNull(batch),
                blankToNull(branch), courseId, pageable);
        if (page.isEmpty()) return page.map(u -> null);

        List<UUID> ids = page.getContent().stream().map(User::getId).toList();

        Map<UUID, StudentProfile> profiles = studentProfileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        Map<UUID, StudentEnrollmentAggProjection> enrollAgg = enrollmentRepository.studentEnrollmentAgg(ids).stream()
                .collect(Collectors.toMap(StudentEnrollmentAggProjection::getStudentId, Function.identity(), (a, b) -> a));
        Map<UUID, StudentAttendanceAggProjection> attAgg = attendanceRepository.studentAttendanceAgg(ids).stream()
                .collect(Collectors.toMap(StudentAttendanceAggProjection::getStudentId, Function.identity(), (a, b) -> a));
        Map<UUID, StudentAssignmentAggProjection> assignAgg = submissionRepository.studentAssignmentAgg(ids).stream()
                .collect(Collectors.toMap(StudentAssignmentAggProjection::getStudentId, Function.identity(), (a, b) -> a));
        Map<UUID, Long> totalAssignments = enrollmentRepository.totalAssignmentsForStudents(ids, PAID_STATUSES).stream()
                .collect(Collectors.toMap(IdCountProjection::getId, IdCountProjection::getCnt, (a, b) -> a));

        return page.map(u -> {
            StudentProfile p = profiles.get(u.getId());
            StudentEnrollmentAggProjection e = enrollAgg.get(u.getId());
            StudentAttendanceAggProjection a = attAgg.get(u.getId());
            StudentAssignmentAggProjection s = assignAgg.get(u.getId());

            long courses = e == null ? 0 : e.getCourseCount();
            double avgProgress = e == null || e.getAvgProgress() == null ? 0 : round2(e.getAvgProgress());
            long totalSessions = a == null ? 0 : a.getTotalSessions();
            long present = a == null ? 0 : a.getPresentSessions();
            double attendancePct = totalSessions == 0 ? 0 : round2((present * 100.0) / totalSessions);
            long submitted = s == null ? 0 : s.getSubmittedCount();
            double avgScore = s == null || s.getAvgScore() == null ? 0 : round2(s.getAvgScore());
            long total = totalAssignments.getOrDefault(u.getId(), 0L);
            long pending = Math.max(0, total - submitted);

            return new StudentReportRowResponse(
                    u.getId(), u.getStudentId(), u.getFullName(), u.getEmail(),
                    p == null ? null : p.getBatch(), p == null ? null : p.getBranch(), p == null ? null : p.getCollege(),
                    courses, avgProgress, attendancePct, avgScore, submitted, pending
            );
        });
    }

    // =====================================================================
    // Faculty Report
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<FacultyReportRowResponse> facultyReport(String search, Pageable pageable) {
        Page<FacultyCourseStatsProjection> page = courseRepository.facultyCourseStats(blankToNull(search), pageable);
        if (page.isEmpty()) return page.map(x -> null);

        List<String> names = page.getContent().stream().map(FacultyCourseStatsProjection::getInstructorName).toList();
        Map<String, FacultyEnrollmentStatsProjection> enrollAgg = enrollmentRepository.facultyEnrollmentAgg(names, PAID_STATUSES)
                .stream().collect(Collectors.toMap(FacultyEnrollmentStatsProjection::getInstructorName, Function.identity(), (a, b) -> a));

        return page.map(c -> {
            FacultyEnrollmentStatsProjection e = enrollAgg.get(c.getInstructorName());
            return new FacultyReportRowResponse(
                    c.getInstructorName(),
                    c.getCoursesHandled(),
                    e == null ? 0 : e.getTotalEnrollments(),
                    c.getAvgRating() == null ? 0 : round2(c.getAvgRating()),
                    e == null || e.getAvgCompletion() == null ? 0 : round2(e.getAvgCompletion()),
                    e == null || e.getRevenue() == null ? BigDecimal.ZERO : e.getRevenue()
            );
        });
    }

    // =====================================================================
    // Attendance Report
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<AttendanceReportRowResponse> attendanceReport(UUID courseId, LocalDate from, LocalDate to, Pageable pageable) {
        return attendanceRepository.attendanceByCourse(courseId, from, to, pageable).map(r -> {
            long present = r.getPresentCount();
            long absent = r.getAbsentCount();
            long late = r.getLateCount();
            long excused = r.getExcusedCount();
            long totalMarks = present + absent + late + excused;
            double pct = totalMarks == 0 ? 0 : round2((present * 100.0) / totalMarks);
            return new AttendanceReportRowResponse(r.getCourseId(), r.getCourseTitle(), r.getSessionsHeld(),
                    present, absent, late, excused, pct);
        });
    }

    // =====================================================================
    // Assignment Report
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<AssignmentReportRowResponse> assignmentReport(UUID courseId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<Assignment> page = assignmentRepository.searchForReport(courseId, from, to, pageable);
        if (page.isEmpty()) return page.map(x -> null);

        List<UUID> assignmentIds = page.getContent().stream().map(Assignment::getId).toList();
        List<UUID> courseIds = page.getContent().stream()
                .map(Assignment::getCourse).filter(java.util.Objects::nonNull)
                .map(com.rrtechnosoft.lms.entity.Course::getId).distinct().toList();

        Map<UUID, AssignmentReportAggProjection> stats = submissionRepository.assignmentReportAgg(assignmentIds).stream()
                .collect(Collectors.toMap(AssignmentReportAggProjection::getAssignmentId, Function.identity(), (a, b) -> a));
        Map<UUID, Long> totalStudentsByCourse = courseIds.isEmpty() ? Map.of() : enrollmentRepository
                .countByCourseIdsAndStatuses(courseIds, PAID_STATUSES).stream()
                .collect(Collectors.toMap(IdCountProjection::getId, IdCountProjection::getCnt, (a, b) -> a));

        return page.map(a -> {
            AssignmentReportAggProjection s = stats.get(a.getId());
            long submitted = s == null ? 0 : s.getSubmittedCount();
            long graded = s == null ? 0 : s.getGradedCount();
            long late = s == null ? 0 : s.getLateCount();
            double avgScore = s == null || s.getAvgScore() == null ? 0 : round2(s.getAvgScore());
            long totalStudents = a.getCourse() == null ? 0 : totalStudentsByCourse.getOrDefault(a.getCourse().getId(), 0L);
            long pending = Math.max(0, totalStudents - submitted);
            double rate = totalStudents == 0 ? 0 : round2((submitted * 100.0) / totalStudents);

            return new AssignmentReportRowResponse(
                    a.getId(), a.getTitle(), a.getCourse() == null ? null : a.getCourse().getTitle(), a.getDueAt(),
                    totalStudents, submitted, graded, late, pending, avgScore, rate
            );
        });
    }

    // =====================================================================
    // Revenue Report
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<RevenueReportRowResponse> revenueReport(UUID courseId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return enrollmentRepository.revenueByCourse(courseId, from, to, pageable).map(r -> {
            BigDecimal unitPrice = r.getUnitPrice() == null ? BigDecimal.ZERO : r.getUnitPrice();
            long paid = r.getPaidEnrollments() == null ? 0 : r.getPaidEnrollments();
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(paid));
            return new RevenueReportRowResponse(
                    r.getCourseId(), r.getCourseTitle(), r.getCategory(), unitPrice,
                    paid, r.getDroppedOrPendingEnrollments() == null ? 0 : r.getDroppedOrPendingEnrollments(), total
            );
        });
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse revenueSummary(OffsetDateTime from, OffsetDateTime to) {
        BigDecimal totalRevenue = enrollmentRepository.sumRevenueByStatusesAndDateRange(PAID_STATUSES, from, to);
        long totalPaid = enrollmentRepository.countByStatusesAndDateRange(PAID_STATUSES, from, to);
        BigDecimal aov = totalPaid == 0 ? BigDecimal.ZERO : totalRevenue.divide(BigDecimal.valueOf(totalPaid), 2, RoundingMode.HALF_UP);
        OffsetDateTime since = from != null ? from : OffsetDateTime.now(ZoneOffset.UTC).minusMonths(6);
        List<TrendPointResponse> trend = enrollmentRepository.monthlyRevenue(since).stream()
                .map(r -> TrendPointResponse.of(r.getMonthLabel(), r.getVal()))
                .toList();
        return new RevenueSummaryResponse(totalRevenue, totalPaid, aov, trend);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
