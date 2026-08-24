package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.PlacementApplication;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlacementApplicationRepository extends JpaRepository<PlacementApplication, UUID> {

    boolean existsByPlacement_IdAndStudent_Id(UUID placementId, UUID studentId);

    Optional<PlacementApplication> findByPlacement_IdAndStudent_Id(UUID placementId, UUID studentId);

    @Query("""
        select a from PlacementApplication a
        where a.placement.id = :placementId
          and (:status is null or a.status = :status)
        order by a.appliedAt desc
        """)
    Page<PlacementApplication> findByPlacement(@Param("placementId") UUID placementId,
                                                @Param("status") ApplicationStatus status,
                                                Pageable pageable);

    Page<PlacementApplication> findByStudent_IdOrderByAppliedAtDesc(UUID studentId, Pageable pageable);

    List<PlacementApplication> findByStudent_Id(UUID studentId);

    long countByStatus(ApplicationStatus status);

    long countByPlacement_Id(UUID placementId);
}
