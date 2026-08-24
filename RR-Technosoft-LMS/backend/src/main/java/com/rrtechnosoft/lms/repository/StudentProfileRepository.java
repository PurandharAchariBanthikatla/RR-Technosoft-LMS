package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserId(UUID userId);

    // Reports & Analytics module — batched fetch for the Student Report page.
    List<StudentProfile> findByUserIdIn(List<UUID> userIds);
}
