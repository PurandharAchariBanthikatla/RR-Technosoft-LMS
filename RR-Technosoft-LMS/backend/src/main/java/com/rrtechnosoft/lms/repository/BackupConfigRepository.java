package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BackupConfigRepository extends JpaRepository<BackupConfig, UUID> {
    Optional<BackupConfig> findBySingletonGuardTrue();
}
