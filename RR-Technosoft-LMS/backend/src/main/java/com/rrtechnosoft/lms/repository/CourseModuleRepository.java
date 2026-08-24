package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findByCourseIdOrderByPositionAsc(UUID courseId);

    long countByCourseId(UUID courseId);

    @Query("select coalesce(max(m.position), -1) from CourseModule m where m.course.id = :courseId")
    int findMaxPosition(@Param("courseId") UUID courseId);

    @Query("select l.module.id as id, count(l) as cnt from Lesson l where l.module.id in :moduleIds group by l.module.id")
    List<IdCountProjection> countLessonsByModuleIds(@Param("moduleIds") List<UUID> moduleIds);
}
