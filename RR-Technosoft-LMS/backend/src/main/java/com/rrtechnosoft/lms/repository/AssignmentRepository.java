package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @EntityGraph(attributePaths = "course")
    @Query("""
        select a from Assignment a
        where (:courseId is null or a.course.id = :courseId)
        order by a.dueAt asc nulls last, a.createdAt desc
        """)
    Page<Assignment> search(@Param("courseId") UUID courseId, Pageable pageable);

    // Assignment Report — same shape as search() plus a due-date range filter.
    @EntityGraph(attributePaths = "course")
    @Query("""
        select a from Assignment a
        where (:courseId is null or a.course.id = :courseId)
          and (:from is null or a.dueAt >= :from)
          and (:to is null or a.dueAt <= :to)
        order by a.dueAt asc nulls last, a.createdAt desc
        """)
    Page<Assignment> searchForReport(@Param("courseId") UUID courseId,
                                      @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to,
                                      Pageable pageable);
}
