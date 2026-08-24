package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdOrderByPositionAsc(UUID moduleId);

    long countByModuleId(UUID moduleId);

    @Query("select coalesce(max(l.position), -1) from Lesson l where l.module.id = :moduleId")
    int findMaxPosition(@Param("moduleId") UUID moduleId);
}
