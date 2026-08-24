package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByStudentId(String studentId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentId(String studentId);

    long countByRole(UserRole role);

    long countByStudentIdStartingWith(String prefix);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByRoleAndFullNameContainingIgnoreCase(UserRole role, String name, Pageable pageable);

    // ---------------------------------------------------------------------
    // Reports & Analytics module — Student Report.
    // StudentProfile is a shared-PK side table (no back-reference on User),
    // so it's joined ad hoc on user id rather than navigated as an association.
    // courseId filters to students with an (ACTIVE/COMPLETED) enrollment in that course.
    // ---------------------------------------------------------------------
    @Query(value = """
        select u from User u
        left join StudentProfile sp on sp.userId = u.id
        where u.role = com.rrtechnosoft.lms.entity.enums.UserRole.STUDENT
          and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                                or lower(u.studentId) like lower(concat('%', :search, '%')))
          and (:batch is null or sp.batch = :batch)
          and (:branch is null or sp.branch = :branch)
          and (:courseId is null or exists (
                select 1 from Enrollment e
                where e.student = u and e.course.id = :courseId
                  and e.status in (com.rrtechnosoft.lms.entity.enums.EnrollmentStatus.ACTIVE,
                                    com.rrtechnosoft.lms.entity.enums.EnrollmentStatus.COMPLETED)
          ))
        order by u.fullName asc
        """,
        countQuery = """
        select count(u) from User u
        left join StudentProfile sp on sp.userId = u.id
        where u.role = com.rrtechnosoft.lms.entity.enums.UserRole.STUDENT
          and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                                or lower(u.studentId) like lower(concat('%', :search, '%')))
          and (:batch is null or sp.batch = :batch)
          and (:branch is null or sp.branch = :branch)
          and (:courseId is null or exists (
                select 1 from Enrollment e
                where e.student = u and e.course.id = :courseId
                  and e.status in (com.rrtechnosoft.lms.entity.enums.EnrollmentStatus.ACTIVE,
                                    com.rrtechnosoft.lms.entity.enums.EnrollmentStatus.COMPLETED)
          ))
        """)
    Page<User> searchStudentsForReport(@Param("search") String search,
                                        @Param("batch") String batch,
                                        @Param("branch") String branch,
                                        @Param("courseId") UUID courseId,
                                        Pageable pageable);
}
