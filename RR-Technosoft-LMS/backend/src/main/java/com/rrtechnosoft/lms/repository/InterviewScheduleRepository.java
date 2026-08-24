package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, UUID> {

    List<InterviewSchedule> findByApplication_IdOrderByRoundNumberAsc(UUID applicationId);

    @Query("""
        select i from InterviewSchedule i
        where i.application.student.id = :studentId
        order by i.scheduledAt asc
        """)
    List<InterviewSchedule> findByStudentId(@Param("studentId") UUID studentId);

    @Query("""
        select i from InterviewSchedule i
        where i.status = com.rrtechnosoft.lms.entity.enums.InterviewStatus.SCHEDULED
          and i.scheduledAt between :from and :to
        order by i.scheduledAt asc
        """)
    List<InterviewSchedule> findScheduledBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    long countByStatus(com.rrtechnosoft.lms.entity.enums.InterviewStatus status);
}
