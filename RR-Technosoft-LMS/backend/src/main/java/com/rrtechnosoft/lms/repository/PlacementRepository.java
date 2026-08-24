package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Placement;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlacementRepository extends JpaRepository<Placement, UUID> {

    @Query("""
        select p from Placement p
        where (:search is null or lower(p.companyName) like lower(concat('%', :search, '%'))
                                or lower(p.roleTitle) like lower(concat('%', :search, '%')))
          and (:status is null or p.status = :status)
          and (:companyId is null or p.company.id = :companyId)
        order by p.createdAt desc
        """)
    Page<Placement> search(@Param("search") String search,
                            @Param("status") PlacementStatus status,
                            @Param("companyId") UUID companyId,
                            Pageable pageable);

    @Query("select p from Placement p where p.status = com.rrtechnosoft.lms.entity.enums.PlacementStatus.OPEN order by p.driveDate asc nulls last")
    List<Placement> findUpcomingOpen(Pageable pageable);

    long countByStatus(PlacementStatus status);

    @Query("select a.placement.id as id, count(a) as cnt from PlacementApplication a where a.placement.id in :placementIds group by a.placement.id")
    List<IdCountProjection> countApplicationsByPlacementIds(@Param("placementIds") List<UUID> placementIds);

    @Query("select count(a) from PlacementApplication a where a.placement.id = :placementId")
    long countApplications(@Param("placementId") UUID placementId);

    long countByDriveDateBetween(LocalDate from, LocalDate to);
}
