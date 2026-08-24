package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.PracticeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PracticeSubmissionRepository extends JpaRepository<PracticeSubmission, UUID> {

    List<PracticeSubmission> findByProblem_IdAndStudent_IdOrderBySubmittedAtDesc(UUID problemId, UUID studentId);

    /**
     * Per-problem submission stats for a batch of problem ids — used by the
     * list endpoint to compute each problem's success rate in one query
     * instead of one query per row on the page.
     */
    @Query("""
        select s.problem.id as problemId,
               count(s) as totalCount,
               sum(case when s.isCorrect = true then 1L else 0L end) as correctCount
        from PracticeSubmission s
        where s.problem.id in :problemIds
        group by s.problem.id
        """)
    List<ProblemStats> findStatsForProblems(@Param("problemIds") List<UUID> problemIds);

    /** Problem ids the given student has at least one correct submission for, among the given batch. */
    @Query("""
        select distinct s.problem.id
        from PracticeSubmission s
        where s.student.id = :studentId
          and s.problem.id in :problemIds
          and s.isCorrect = true
        """)
    List<UUID> findSolvedProblemIds(@Param("studentId") UUID studentId, @Param("problemIds") List<UUID> problemIds);

    interface ProblemStats {
        UUID getProblemId();
        Long getTotalCount();
        Long getCorrectCount();
    }
}
