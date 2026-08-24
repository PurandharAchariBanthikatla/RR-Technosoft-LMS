package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("""
        select c from Course c
        where (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
          and (:status is null or c.status = :status)
          and (:category is null or c.category = :category)
        """)
    Page<Course> search(@Param("search") String search,
                         @Param("status") CourseStatus status,
                         @Param("category") String category,
                         Pageable pageable);

    // --- Batched counts, used to build list/detail DTOs without N+1 queries ---

    @Query("select m.course.id as id, count(m) as cnt from CourseModule m where m.course.id in :courseIds group by m.course.id")
    List<IdCountProjection> countModulesByCourseIds(@Param("courseIds") List<UUID> courseIds);

    // Enrollment module has shipped (see EnrollmentRepository) — this stays a
    // JPQL aggregate scoped to CourseRepository's own read path rather than
    // pulling EnrollmentRepository in as a dependency here.
    @Query("select e.course.id as id, count(e) as cnt from Enrollment e where e.course.id in :courseIds group by e.course.id")
    List<IdCountProjection> countEnrollmentsByCourseIds(@Param("courseIds") List<UUID> courseIds);

    @Query("select count(m) from CourseModule m where m.course.id = :courseId")
    long countModules(@Param("courseId") UUID courseId);

    @Query("select count(e) from Enrollment e where e.course.id = :courseId")
    long countEnrollments(@Param("courseId") UUID courseId);

    // ---------------------------------------------------------------------
    // Reports & Analytics module.
    // ---------------------------------------------------------------------

    long countByStatus(CourseStatus status);

    @Query("select count(distinct c.instructorName) from Course c where c.instructorName is not null")
    long countDistinctInstructors();

    // Faculty Report — one row per distinct instructor name, paginated.
    // Course has no dedicated Faculty entity/role yet, so instructorName is
    // the grouping key (see FacultyReportRowResponse javadoc).
    @Query(value = """
        select c.instructorName as instructorName, count(c) as coursesHandled, avg(c.rating) as avgRating
        from Course c
        where c.instructorName is not null
          and (:search is null or lower(c.instructorName) like lower(concat('%', :search, '%')))
        group by c.instructorName
        order by c.instructorName asc
        """,
        countQuery = """
        select count(distinct c.instructorName) from Course c
        where c.instructorName is not null
          and (:search is null or lower(c.instructorName) like lower(concat('%', :search, '%')))
        """)
    Page<FacultyCourseStatsProjection> facultyCourseStats(@Param("search") String search, Pageable pageable);

    // Dashboard "course category distribution" chart — courses + enrollments per category.
    @Query("""
        select new com.rrtechnosoft.lms.dto.response.reports.CourseDistributionResponse(
                   c.category, count(distinct c), count(e))
        from Course c left join Enrollment e on e.course = c
        group by c.category
        order by c.category asc
        """)
    List<com.rrtechnosoft.lms.dto.response.reports.CourseDistributionResponse> courseDistributionRaw();
}
