package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.StudentFee;
import com.rrtechnosoft.lms.entity.enums.FeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentFeeRepository extends JpaRepository<StudentFee, UUID> {

    @EntityGraph(attributePaths = {"student", "course", "feeStructure", "installments"})
    @Query("""
        select sf from StudentFee sf
        where (:studentId is null or sf.student.id = :studentId)
          and (:courseId is null or sf.course.id = :courseId)
          and (:status is null or sf.status = :status)
        order by sf.createdAt desc
        """)
    Page<StudentFee> search(@Param("studentId") UUID studentId,
                             @Param("courseId") UUID courseId,
                             @Param("status") FeeStatus status,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"student", "course", "feeStructure", "installments"})
    Optional<StudentFee> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"student", "course", "feeStructure", "installments"})
    List<StudentFee> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    @Query("select coalesce(sum(sf.netPayable),0) from StudentFee sf where (:courseId is null or sf.course.id = :courseId)")
    BigDecimal totalBilled(@Param("courseId") UUID courseId);

    @Query("select coalesce(sum(sf.amountPaid),0) from StudentFee sf where (:courseId is null or sf.course.id = :courseId)")
    BigDecimal totalCollected(@Param("courseId") UUID courseId);

    @Query("select count(sf) from StudentFee sf where sf.status = com.rrtechnosoft.lms.entity.enums.FeeStatus.OVERDUE and (:courseId is null or sf.course.id = :courseId)")
    long countOverdue(@Param("courseId") UUID courseId);

    @Query("select count(sf) from StudentFee sf where (:courseId is null or sf.course.id = :courseId)")
    long countTotal(@Param("courseId") UUID courseId);
}
