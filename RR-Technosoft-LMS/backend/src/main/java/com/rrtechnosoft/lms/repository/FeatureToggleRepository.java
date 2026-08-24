package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.FeatureToggle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, UUID> {
    List<FeatureToggle> findAllByOrderByNameAsc();
    Optional<FeatureToggle> findByFeatureKey(String featureKey);
    boolean existsByFeatureKeyAndEnabledTrue(String featureKey);
}
