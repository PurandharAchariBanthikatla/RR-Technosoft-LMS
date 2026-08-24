package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Attendance;
import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByCourseIdAndStudentIdAndAttendanceDate(UUID courseId, UUID studentId, LocalDate date);

    @EntityGraph(attributePaths = {"course", "student"})
    @Query("""
        select a from Attendance a
        where (:courseId is null or a.course.id = :courseId)
          and (:date is null or a.attendanceDate = :date)
        order by a.attendanceDate desc
        """)
    Page<Attendance> search(@Param("courseId") UUID courseId,
                             @Param("date") LocalDate date,
                             Pageable pageable);

    @EntityGraph(attributePaths = "course")
    @Query("""
        select a from Attendance a
        where a.student.id = :studentId
          and (:from is null or a.attendanceDate >= :from)
          and (:to is null or a.attendanceDate <= :to)
        order by a.attendanceDate desc
        """)
    List<Attendance> findForStudent(@Param("studentId") UUID studentId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    long countByStudentId(UUID studentId);

    long countByStudentIdAndStatus(UUID studentId, AttendanceStatus status);

    // ---------------------------------------------------------------------
    // Reports & Analytics module.
    // ---------------------------------------------------------------------

    // Batched per-student rollup for the Student Report page.
    @Query("""
        select a.student.id as studentId, count(a) as totalSessions,
               sum(case when a.status = com.rrtechnosoft.lms.entity.enums.AttendanceStatus.PRESENT then 1L else 0L end) as presentSessions
        from Attendance a
        where a.student.id in :studentIds
        group by a.student.id
        """)
    List<StudentAttendanceAggProjection> studentAttendanceAgg(@Param("studentIds") List<UUID> studentIds);

    // Attendance Report — one row per course over the requested date range.
    @Query(value = """
        select course_id as courseId, courseTitle,
               count(distinct attendance_date) as sessionsHeld,
               coalesce(sum(case when status = 'PRESENT' then 1 else 0 end), 0) as presentCount,
               coalesce(sum(case when status = 'ABSENT' then 1 else 0 end), 0) as absentCount,
               coalesce(sum(case when status = 'LATE' then 1 else 0 end), 0) as lateCount,
               coalesce(sum(case when status = 'EXCUSED' then 1 else 0 end), 0) as excusedCount
        from (
            select a.course_id, c.title as courseTitle, a.attendance_date, a.status
            from attendance a
            join courses c on c.id = a.course_id
            where (:courseId is null or a.course_id = :courseId)
              and (:from is null or a.attendance_date >= :from)
              and (:to is null or a.attendance_date <= :to)
        ) x
        group by course_id, courseTitle
        order by courseTitle asc
        """,
        countQuery = """
        select count(distinct a.course_id) from attendance a
        where (:courseId is null or a.course_id = :courseId)
          and (:from is null or a.attendance_date >= :from)
          and (:to is null or a.attendance_date <= :to)
        """,
        nativeQuery = true)
    Page<AttendanceReportAggProjection> attendanceByCourse(@Param("courseId") UUID courseId,
                                                             @Param("from") LocalDate from,
                                                             @Param("to") LocalDate to,
                                                             Pageable pageable);

    // Monthly average attendance percentage — dashboard "attendance trend" chart.
    @Query(value = """
        select to_char(date_trunc('month', attendance_date), 'YYYY-MM') as monthLabel,
               round(100.0 * sum(case when status = 'PRESENT' then 1 else 0 end) / count(*), 2) as val
        from attendance
        where attendance_date >= :since
        group by 1
        order by 1
        """, nativeQuery = true)
    List<MonthValueProjection> monthlyAttendancePercentage(@Param("since") LocalDate since);

    // Overall average attendance percentage — dashboard KPI.
    @Query(value = """
        select coalesce(round(100.0 * sum(case when status = 'PRESENT' then 1 else 0 end) / nullif(count(*), 0), 2), 0)
        from attendance
        """, nativeQuery = true)
    double overallAttendancePercentage();
}
