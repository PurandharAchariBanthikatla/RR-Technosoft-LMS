package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.FeeStructure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    @EntityGraph(attributePaths = {"course", "installments"})
    @Query("""
        select f from FeeStructure f
        where (:courseId is null or f.course.id = :courseId)
          and (:activeOnly = false or f.isActive = true)
        order by f.createdAt desc
        """)
    Page<FeeStructure> search(@Param("courseId") UUID courseId, @Param("activeOnly") boolean activeOnly, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "installments"})
    java.util.Optional<FeeStructure> findWithInstallmentsById(UUID id);
}
