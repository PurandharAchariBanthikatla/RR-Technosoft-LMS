package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyTaskRepository extends JpaRepository<DailyTask, UUID> {

    @Query("select t from DailyTask t where (:date is null or t.taskDate = :date) order by t.taskDate desc")
    List<DailyTask> search(@Param("date") LocalDate date);
}
