package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.SecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, UUID> {
    Optional<SecuritySettings> findBySingletonGuardTrue();
}
