package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.LiveClass;
import com.rrtechnosoft.lms.entity.enums.LiveClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LiveClassRepository extends JpaRepository<LiveClass, UUID> {

    @EntityGraph(attributePaths = "course")
    @Query("""
        select lc from LiveClass lc
        where (:status is null or lc.status = :status)
          and (:courseId is null or lc.course.id = :courseId)
        order by lc.scheduledStart desc
        """)
    Page<LiveClass> search(@Param("status") LiveClassStatus status,
                            @Param("courseId") UUID courseId,
                            Pageable pageable);

    @EntityGraph(attributePaths = "course")
    @Query("""
        select lc from LiveClass lc
        where lc.status in ('SCHEDULED','LIVE') and lc.scheduledEnd >= :now
        order by lc.scheduledStart asc
        """)
    List<LiveClass> findUpcoming(@Param("now") OffsetDateTime now);

    // Reports & Analytics dashboard — "live classes this week" KPI.
    @Query("""
        select count(lc) from LiveClass lc
        where lc.status in (com.rrtechnosoft.lms.entity.enums.LiveClassStatus.SCHEDULED,
                             com.rrtechnosoft.lms.entity.enums.LiveClassStatus.LIVE)
          and lc.scheduledStart between :from and :to
        """)
    long countUpcomingBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
