package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.SystemSetting;
import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {
    Optional<SystemSetting> findBySettingKey(String settingKey);
    List<SystemSetting> findByCategoryOrderBySettingKeyAsc(SettingCategory category);
    List<SystemSetting> findAllByOrderByCategoryAscSettingKeyAsc();
    boolean existsBySettingKey(String settingKey);
}
