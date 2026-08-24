package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    // Explicit left joins (not @EntityGraph) because module_id/course are
    // nullable — an inner-join-shaped path expression in WHERE would silently
    // drop quizzes with no module even when courseId isn't being filtered on.
    @Query("""
        select distinct q from Quiz q
        left join fetch q.module m
        left join fetch m.course c
        where (:courseId is null or c.id = :courseId)
        order by q.createdAt desc
        """)
    Page<Quiz> search(@Param("courseId") UUID courseId, Pageable pageable);

    @Query("""
        select q from Quiz q
        left join fetch q.module m
        left join fetch m.course c
        where q.id = :id
        """)
    java.util.Optional<Quiz> findByIdWithCourse(@Param("id") UUID id);
}
