package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.BackupRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupRunRepository extends JpaRepository<BackupRun, UUID> {
    Page<BackupRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<BackupRun> findFirstByOrderByStartedAtDesc();

    List<BackupRun> findAllByStartedAtBefore(OffsetDateTime cutoff);
}
