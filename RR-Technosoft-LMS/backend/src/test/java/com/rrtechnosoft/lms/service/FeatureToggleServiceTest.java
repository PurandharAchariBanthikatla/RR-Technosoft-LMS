package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateFeatureToggleRequest;
import com.rrtechnosoft.lms.dto.response.FeatureToggleResponse;
import com.rrtechnosoft.lms.entity.FeatureToggle;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.FeatureToggleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Feature toggles gate whether other admin/student screens are reachable at
 * all (see @Cacheable on list()/isEnabled() — a caching bug here would mean
 * a flipped toggle silently doesn't take effect), so it's worth pinning down
 * that update() actually flips the entity, evicts correctly (annotation-level,
 * not directly testable via Mockito, but the underlying write is), and that
 * an unknown feature key fails loudly instead of silently no-op-ing.
 */
@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    @Mock private FeatureToggleRepository featureToggleRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private FeatureToggleService featureToggleService;

    @Test
    void list_returnsAllTogglesOrderedByName() {
        FeatureToggle toggle = FeatureToggle.builder().id(UUID.randomUUID()).featureKey("practice_portal")
                .name("Practice Portal").enabled(true).build();
        when(featureToggleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(toggle));

        List<FeatureToggleResponse> result = featureToggleService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).featureKey()).isEqualTo("practice_portal");
    }

    @Test
    void isEnabled_delegatesToTheExistsQuery() {
        when(featureToggleRepository.existsByFeatureKeyAndEnabledTrue("ai_chatbot")).thenReturn(true);

        assertThat(featureToggleService.isEnabled("ai_chatbot")).isTrue();
    }

    @Test
    void isEnabled_returnsFalseForAnUnknownOrDisabledKey() {
        when(featureToggleRepository.existsByFeatureKeyAndEnabledTrue("nonexistent")).thenReturn(false);

        assertThat(featureToggleService.isEnabled("nonexistent")).isFalse();
    }

    @Test
    void update_flipsTheToggleAndAudits() {
        UUID actorId = UUID.randomUUID();
        FeatureToggle toggle = FeatureToggle.builder().id(UUID.randomUUID()).featureKey("ai_chatbot")
                .name("AI Assistant").enabled(false).build();
        when(featureToggleRepository.findByFeatureKey("ai_chatbot")).thenReturn(Optional.of(toggle));

        FeatureToggleResponse response = featureToggleService.update(
                "ai_chatbot", new UpdateFeatureToggleRequest(true), actorId);

        assertThat(response.enabled()).isTrue();
        assertThat(toggle.getEnabled()).isTrue();
        assertThat(toggle.getUpdatedBy()).isEqualTo(actorId);
        verify(featureToggleRepository).save(toggle);
        verify(auditLogService).log(actorId, "ENABLE_FEATURE", "FeatureToggle", toggle.getId(), null);
    }

    @Test
    void update_logsDisableFeatureWhenTurningOff() {
        UUID actorId = UUID.randomUUID();
        FeatureToggle toggle = FeatureToggle.builder().id(UUID.randomUUID()).featureKey("ai_chatbot")
                .name("AI Assistant").enabled(true).build();
        when(featureToggleRepository.findByFeatureKey("ai_chatbot")).thenReturn(Optional.of(toggle));

        featureToggleService.update("ai_chatbot", new UpdateFeatureToggleRequest(false), actorId);

        verify(auditLogService).log(actorId, "DISABLE_FEATURE", "FeatureToggle", toggle.getId(), null);
    }

    @Test
    void update_throwsNotFoundForAnUnknownFeatureKey() {
        when(featureToggleRepository.findByFeatureKey("does_not_exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featureToggleService.update(
                "does_not_exist", new UpdateFeatureToggleRequest(true), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does_not_exist");
        verify(featureToggleRepository, never()).save(any());
    }
}
