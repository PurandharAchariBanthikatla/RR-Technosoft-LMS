package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    // Batched lookups used to build list-view responses without N+1 queries.
    @Query("select s.assignment.id as id, count(s) as cnt from AssignmentSubmission s where s.assignment.id in :assignmentIds group by s.assignment.id")
    List<IdCountProjection> countByAssignmentIds(@Param("assignmentIds") List<UUID> assignmentIds);

    @EntityGraph(attributePaths = "assignment")
    @Query("select s from AssignmentSubmission s where s.assignment.id in :assignmentIds and s.student.id = :studentId")
    List<AssignmentSubmission> findByAssignmentIdsAndStudentId(@Param("assignmentIds") List<UUID> assignmentIds,
                                                                 @Param("studentId") UUID studentId);

    // ---------------------------------------------------------------------
    // Reports & Analytics module.
    // ---------------------------------------------------------------------

    // Batched per-student rollup (submitted count + avg score) for the Student Report page.
    @Query("""
        select s.student.id as studentId, count(s) as submittedCount, avg(s.score) as avgScore
        from AssignmentSubmission s
        where s.student.id in :studentIds
        group by s.student.id
        """)
    List<StudentAssignmentAggProjection> studentAssignmentAgg(@Param("studentIds") List<UUID> studentIds);

    // Batched per-assignment rollup (submitted/graded/late counts + avg score) for the Assignment Report page.
    @Query("""
        select s.assignment.id as assignmentId,
               count(s) as submittedCount,
               sum(case when s.status = com.rrtechnosoft.lms.entity.enums.SubmissionStatus.GRADED then 1L else 0L end) as gradedCount,
               sum(case when s.status = com.rrtechnosoft.lms.entity.enums.SubmissionStatus.LATE then 1L else 0L end) as lateCount,
               avg(s.score) as avgScore
        from AssignmentSubmission s
        where s.assignment.id in :assignmentIds
        group by s.assignment.id
        """)
    List<AssignmentReportAggProjection> assignmentReportAgg(@Param("assignmentIds") List<UUID> assignmentIds);

    // Reports & Analytics dashboard — submissions awaiting grading.
    @Query("""
        select count(s) from AssignmentSubmission s
        where s.status in (com.rrtechnosoft.lms.entity.enums.SubmissionStatus.SUBMITTED,
                            com.rrtechnosoft.lms.entity.enums.SubmissionStatus.LATE)
        """)
    long countPendingGrading();
}
