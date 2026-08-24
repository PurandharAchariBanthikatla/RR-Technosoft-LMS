package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateSystemSettingValueRequest;
import com.rrtechnosoft.lms.dto.request.UpsertSystemSettingRequest;
import com.rrtechnosoft.lms.dto.response.SystemSettingResponse;
import com.rrtechnosoft.lms.entity.SystemSetting;
import com.rrtechnosoft.lms.entity.enums.SettingCategory;
import com.rrtechnosoft.lms.entity.enums.SettingValueType;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.SystemSettingRepository;
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
 * The one rule worth pinning down here is isEditable: a handful of system
 * settings are meant to be read-only (seeded defaults an admin shouldn't be
 * able to change or delete through the UI), and updateValue()/delete() both
 * gate on that flag — a regression there would let an admin silently corrupt
 * or remove a setting that's supposed to be protected.
 */
@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock private SystemSettingRepository systemSettingRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private SystemSettingService systemSettingService;

    @Test
    void list_filtersByCategoryWhenOneIsProvided() {
        SystemSetting setting = SystemSetting.builder().id(UUID.randomUUID()).settingKey("max_upload_mb")
                .category(SettingCategory.GENERAL).valueType(SettingValueType.NUMBER).build();
        when(systemSettingRepository.findByCategoryOrderBySettingKeyAsc(SettingCategory.GENERAL))
                .thenReturn(List.of(setting));

        List<SystemSettingResponse> result = systemSettingService.list(SettingCategory.GENERAL);

        assertThat(result).hasSize(1);
        verify(systemSettingRepository, never()).findAllByOrderByCategoryAscSettingKeyAsc();
    }

    @Test
    void list_returnsEverythingWhenNoCategoryIsProvided() {
        when(systemSettingRepository.findAllByOrderByCategoryAscSettingKeyAsc()).thenReturn(List.of());

        systemSettingService.list(null);

        verify(systemSettingRepository).findAllByOrderByCategoryAscSettingKeyAsc();
        verify(systemSettingRepository, never()).findByCategoryOrderBySettingKeyAsc(any());
    }

    @Test
    void create_savesANewSettingAndAudits() {
        UUID actorId = UUID.randomUUID();
        UpsertSystemSettingRequest request = new UpsertSystemSettingRequest(
                "session_timeout_minutes", "30", SettingValueType.NUMBER, SettingCategory.SECURITY, "Idle session timeout");
        when(systemSettingRepository.existsBySettingKey("session_timeout_minutes")).thenReturn(false);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(inv -> {
            SystemSetting s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SystemSettingResponse response = systemSettingService.create(request, actorId);

        assertThat(response.settingValue()).isEqualTo("30");
        verify(auditLogService).log(eq(actorId), eq("CREATE_SYSTEM_SETTING"), eq("SystemSetting"), any(), isNull());
    }

    @Test
    void create_throwsConflictWhenTheKeyAlreadyExists() {
        UpsertSystemSettingRequest request = new UpsertSystemSettingRequest(
                "session_timeout_minutes", "30", SettingValueType.NUMBER, SettingCategory.SECURITY, null);
        when(systemSettingRepository.existsBySettingKey("session_timeout_minutes")).thenReturn(true);

        assertThatThrownBy(() -> systemSettingService.create(request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateValue_updatesAnEditableSetting() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        SystemSetting setting = SystemSetting.builder().id(id).settingKey("max_upload_mb")
                .settingValue("25").isEditable(true).build();
        when(systemSettingRepository.findById(id)).thenReturn(Optional.of(setting));

        SystemSettingResponse response = systemSettingService.updateValue(
                id, new UpdateSystemSettingValueRequest("50"), actorId);

        assertThat(response.settingValue()).isEqualTo("50");
        assertThat(setting.getUpdatedBy()).isEqualTo(actorId);
        verify(auditLogService).log(actorId, "UPDATE_SYSTEM_SETTING", "SystemSetting", id, null);
    }

    @Test
    void updateValue_throwsWhenTheSettingIsNotEditable() {
        UUID id = UUID.randomUUID();
        SystemSetting setting = SystemSetting.builder().id(id).settingKey("platform_name")
                .settingValue("RR Technosoft LMS").isEditable(false).build();
        when(systemSettingRepository.findById(id)).thenReturn(Optional.of(setting));

        assertThatThrownBy(() -> systemSettingService.updateValue(
                id, new UpdateSystemSettingValueRequest("Something Else"), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not editable");
        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateValue_throwsNotFoundForAnUnknownId() {
        UUID id = UUID.randomUUID();
        when(systemSettingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemSettingService.updateValue(
                id, new UpdateSystemSettingValueRequest("x"), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void delete_removesAnEditableSettingAndAudits() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        SystemSetting setting = SystemSetting.builder().id(id).settingKey("beta_feature_x").isEditable(true).build();
        when(systemSettingRepository.findById(id)).thenReturn(Optional.of(setting));

        systemSettingService.delete(id, actorId);

        verify(systemSettingRepository).delete(setting);
        verify(auditLogService).log(actorId, "DELETE_SYSTEM_SETTING", "SystemSetting", id, null);
    }

    @Test
    void delete_throwsWhenTheSettingIsNotEditable() {
        UUID id = UUID.randomUUID();
        SystemSetting setting = SystemSetting.builder().id(id).settingKey("platform_name").isEditable(false).build();
        when(systemSettingRepository.findById(id)).thenReturn(Optional.of(setting));

        assertThatThrownBy(() -> systemSettingService.delete(id, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be deleted");
        verify(systemSettingRepository, never()).delete(any());
    }
}
