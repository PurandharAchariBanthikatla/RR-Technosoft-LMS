package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {
    Optional<NotificationSettings> findBySingletonGuardTrue();
}
