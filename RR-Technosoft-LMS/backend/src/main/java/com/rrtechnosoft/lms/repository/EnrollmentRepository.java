package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Enrollment;
import com.rrtechnosoft.lms.entity.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(UUID studentId, UUID courseId, EnrollmentStatus status);

    // Fetch-joins student+course in one query — list/mine endpoints render
    // studentName/courseTitle directly, so this avoids per-row N+1 lookups.
    @EntityGraph(attributePaths = {"student", "course"})
    @Query("""
        select e from Enrollment e
        where (:status is null or e.status = :status)
          and (:courseId is null or e.course.id = :courseId)
        order by e.enrolledAt desc
        """)
    Page<Enrollment> search(@Param("status") EnrollmentStatus status,
                             @Param("courseId") UUID courseId,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);

    // Used by AssignmentService to compute "submittedCount/totalStudents" —
    // counts enrollments in the given statuses per course in one batched query.
    @Query("""
        select e.course.id as id, count(e) as cnt from Enrollment e
        where e.course.id in :courseIds and e.status in :statuses
        group by e.course.id
        """)
    List<IdCountProjection> countByCourseIdsAndStatuses(@Param("courseIds") List<UUID> courseIds,
                                                          @Param("statuses") List<EnrollmentStatus> statuses);

    // ---------------------------------------------------------------------
    // Reports & Analytics module — see ReportsService for how these compose.
    // ---------------------------------------------------------------------

    long countByStatus(EnrollmentStatus status);

    long countByStatusIn(List<EnrollmentStatus> statuses);

    // Revenue is derived from paid enrollments (ACTIVE/COMPLETED) x course
    // price. This predates the Finance module's Payment entity (see
    // finance_module migration) — ReportsService still reports off
    // Enrollment/Course rather than Payment so its numbers stay meaningful
    // even for manually-recorded or pre-gateway enrollments. A future pass
    // could reconcile this against Payment.amount for exact parity.
    @Query("select coalesce(sum(e.course.price), 0) from Enrollment e where e.status in :statuses")
    BigDecimal sumRevenueByStatuses(@Param("statuses") List<EnrollmentStatus> statuses);

    @Query("""
        select coalesce(sum(e.course.price), 0) from Enrollment e
        where e.status in :statuses
          and (:from is null or e.enrolledAt >= :from)
          and (:to is null or e.enrolledAt <= :to)
        """)
    BigDecimal sumRevenueByStatusesAndDateRange(@Param("statuses") List<EnrollmentStatus> statuses,
                                                 @Param("from") OffsetDateTime from,
                                                 @Param("to") OffsetDateTime to);

    // Monthly new-enrollment counts for the last N months — dashboard "student growth" chart.
    @Query(value = """
        select to_char(date_trunc('month', enrolled_at), 'YYYY-MM') as monthLabel,
               count(*)::numeric as val
        from enrollments
        where enrolled_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<MonthValueProjection> monthlyEnrollmentCounts(@Param("since") OffsetDateTime since);

    // Monthly revenue trend (paid enrollments only) — dashboard/revenue-report chart.
    @Query(value = """
        select to_char(date_trunc('month', e.enrolled_at), 'YYYY-MM') as monthLabel,
               coalesce(sum(c.price), 0) as val
        from enrollments e
        join courses c on c.id = e.course_id
        where e.status in ('ACTIVE', 'COMPLETED')
          and e.enrolled_at >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<MonthValueProjection> monthlyRevenue(@Param("since") OffsetDateTime since);

    // Batched per-student rollup (course count + average progress) for the Student Report page.
    @Query("""
        select e.student.id as studentId, count(e) as courseCount, avg(e.progressPct) as avgProgress
        from Enrollment e
        where e.student.id in :studentIds
        group by e.student.id
        """)
    List<StudentEnrollmentAggProjection> studentEnrollmentAgg(@Param("studentIds") List<UUID> studentIds);

    // Batched per-instructor rollup (total enrollments, avg completion, revenue) for the Faculty Report page.
    @Query("""
        select c.instructorName as instructorName,
               count(e) as totalEnrollments,
               avg(e.progressPct) as avgCompletion,
               coalesce(sum(case when e.status in :paidStatuses then c.price else 0 end), 0) as revenue
        from Enrollment e
        join e.course c
        where c.instructorName in :instructorNames
        group by c.instructorName
        """)
    List<FacultyEnrollmentStatsProjection> facultyEnrollmentAgg(@Param("instructorNames") List<String> instructorNames,
                                                                  @Param("paidStatuses") List<EnrollmentStatus> paidStatuses);

    // Revenue Report — one row per course, paid vs non-paid enrollment counts, filtered by enrolment date range.
    @Query(value = """
        select c.id as courseId, c.title as courseTitle, c.category as category, c.price as unitPrice,
               coalesce(sum(case when e.status in ('ACTIVE','COMPLETED') then 1 else 0 end), 0) as paidEnrollments,
               coalesce(sum(case when e.status in ('DROPPED','PENDING') then 1 else 0 end), 0) as droppedOrPendingEnrollments
        from courses c
        left join enrollments e on e.course_id = c.id
          and (:from is null or e.enrolled_at >= :from)
          and (:to is null or e.enrolled_at <= :to)
        where (:courseId is null or c.id = :courseId)
        group by c.id, c.title, c.category, c.price
        order by c.title asc
        """,
        countQuery = "select count(*) from courses c where (:courseId is null or c.id = :courseId)",
        nativeQuery = true)
    Page<RevenueRowNative> revenueByCourse(@Param("courseId") UUID courseId,
                                            @Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to,
                                            Pageable pageable);

    @Query("select coalesce(avg(e.progressPct), 0) from Enrollment e")
    double overallAvgProgress();

    @Query("""
        select count(e) from Enrollment e
        where e.status in :statuses
          and (:from is null or e.enrolledAt >= :from)
          and (:to is null or e.enrolledAt <= :to)
        """)
    long countByStatusesAndDateRange(@Param("statuses") List<EnrollmentStatus> statuses,
                                      @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    // Total assignments across each student's enrolled courses — used with
    // studentAssignmentAgg's submittedCount to derive "pending" in the Student Report.
    @Query("""
        select e.student.id as id, count(a) as cnt
        from Enrollment e join Assignment a on a.course = e.course
        where e.student.id in :studentIds and e.status in :statuses
        group by e.student.id
        """)
    List<IdCountProjection> totalAssignmentsForStudents(@Param("studentIds") List<UUID> studentIds,
                                                          @Param("statuses") List<EnrollmentStatus> statuses);

    /** Native projection backing {@link #revenueByCourse}; mapped to RevenueReportRowResponse in ReportsService. */
    interface RevenueRowNative {
        UUID getCourseId();
        String getCourseTitle();
        String getCategory();
        BigDecimal getUnitPrice();
        Long getPaidEnrollments();
        Long getDroppedOrPendingEnrollments();
    }
}
