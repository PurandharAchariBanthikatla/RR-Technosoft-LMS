package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.DailyTaskCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTaskCompletionRepository extends JpaRepository<DailyTaskCompletion, UUID> {

    Optional<DailyTaskCompletion> findByTaskIdAndStudentId(UUID taskId, UUID studentId);

    List<DailyTaskCompletion> findByStudentIdAndTaskIdIn(UUID studentId, List<UUID> taskIds);
}
