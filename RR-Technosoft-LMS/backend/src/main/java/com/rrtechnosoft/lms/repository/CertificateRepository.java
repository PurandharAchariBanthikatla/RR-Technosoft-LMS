package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    @EntityGraph(attributePaths = {"student", "course"})
    Optional<Certificate> findByCertificateNo(String certificateNo);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Certificate> findByStudentIdOrderByIssuedAtDesc(UUID studentId);

    @EntityGraph(attributePaths = {"student", "course"})
    Page<Certificate> findAllBy(Pageable pageable);
}
