package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateFeatureToggleRequest;
import com.rrtechnosoft.lms.dto.response.FeatureToggleResponse;
import com.rrtechnosoft.lms.entity.FeatureToggle;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.FeatureToggleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.rrtechnosoft.lms.config.CacheConfig.FEATURE_TOGGLES;
import static com.rrtechnosoft.lms.config.CacheConfig.FEATURE_TOGGLE_STATUS;

@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureToggleRepository featureToggleRepository;
    private final AuditLogService auditLogService;

    @Cacheable(cacheNames = FEATURE_TOGGLES, key = "'all'")
    @Transactional(readOnly = true)
    public List<FeatureToggleResponse> list() {
        return featureToggleRepository.findAllByOrderByNameAsc().stream().map(FeatureToggleResponse::from).toList();
    }

    @Cacheable(cacheNames = FEATURE_TOGGLE_STATUS, key = "#featureKey")
    @Transactional(readOnly = true)
    public boolean isEnabled(String featureKey) {
        return featureToggleRepository.existsByFeatureKeyAndEnabledTrue(featureKey);
    }

    @CacheEvict(cacheNames = {FEATURE_TOGGLES, FEATURE_TOGGLE_STATUS}, allEntries = true)
    @Transactional
    public FeatureToggleResponse update(String featureKey, UpdateFeatureToggleRequest request, UUID actorId) {
        FeatureToggle toggle = featureToggleRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> ApiException.notFound("Feature toggle not found: " + featureKey));
        toggle.setEnabled(request.enabled());
        toggle.setUpdatedBy(actorId);
        featureToggleRepository.save(toggle);
        auditLogService.log(actorId, request.enabled() ? "ENABLE_FEATURE" : "DISABLE_FEATURE",
                "FeatureToggle", toggle.getId(), null);
        return FeatureToggleResponse.from(toggle);
    }
}
