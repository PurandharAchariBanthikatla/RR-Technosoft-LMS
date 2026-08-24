package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.entity.AuditLog;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.repository.AuditLogRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, userRepository);
    }

    @Test
    void log_persistsAnAuditLogRowWithGivenFields() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        auditLogService.log(actorId, "CREATE_COURSE", "Course", entityId, "10.0.0.1");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void search_resolvesActorNamesInBatchAndMapsToResponse() {
        UUID actorId = UUID.randomUUID();
        User actor = User.builder().id(actorId).fullName("Priya Admin").email("priya@rrtechnosoft.com").build();

        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .action("DELETE_COURSE")
                .entityType("Course")
                .entityId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 25);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));
        when(userRepository.findAllById(List.of(actorId))).thenReturn(List.of(actor));

        var result = auditLogService.search(null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).actorName()).isEqualTo("Priya Admin");
        assertThat(result.getContent().get(0).actorEmail()).isEqualTo("priya@rrtechnosoft.com");
        assertThat(result.getContent().get(0).action()).isEqualTo("DELETE_COURSE");
    }

    @Test
    void search_fallsBackToSystemWhenActorIsNull() {
        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(null)
                .action("SCHEDULED_BACKUP")
                .createdAt(OffsetDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 25);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));
        when(userRepository.findAllById(List.of())).thenReturn(List.of());

        var result = auditLogService.search(null, null, null, null, null, null, pageable);

        assertThat(result.getContent().get(0).actorName()).isEqualTo("System");
        assertThat(result.getContent().get(0).actorEmail()).isNull();
    }
}
