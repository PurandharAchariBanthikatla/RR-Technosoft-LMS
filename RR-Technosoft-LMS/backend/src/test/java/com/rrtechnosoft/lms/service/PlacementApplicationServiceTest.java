package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.FileUploadResponse;
import com.rrtechnosoft.lms.entity.PlacementApplication;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.PlacementApplicationRepository;
import com.rrtechnosoft.lms.repository.StudentProfileRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers attachResume() — the previously backend-only method that
 * PlacementController now exposes via POST /placements/applications/{id}/resume.
 */
@ExtendWith(MockitoExtension.class)
class PlacementApplicationServiceTest {

    @Mock private PlacementApplicationRepository applicationRepository;
    @Mock private PlacementService placementService;
    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuditLogService auditLogService;

    private PlacementApplicationService service;

    private final UUID studentId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PlacementApplicationService(applicationRepository, placementService, userRepository,
                studentProfileRepository, fileStorageService, auditLogService);
    }

    @Test
    void attachResume_uploadsAndSetsResumeUrlForOwner() {
        User owner = User.builder().id(studentId).build();
        PlacementApplication application = PlacementApplication.builder().id(applicationId).student(owner).build();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(fileStorageService.upload(any(), eq("resumes"), any()))
                .thenReturn(new FileUploadResponse("https://cdn.example.com/resumes/abc.pdf", "resumes/abc.pdf", 12345L));
        when(applicationRepository.save(any(PlacementApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3});
        var response = service.attachResume(applicationId, file, studentId);

        assertThat(response.resumeUrl()).isEqualTo("https://cdn.example.com/resumes/abc.pdf");
        verify(auditLogService).log(studentId, "PLACEMENT_APPLICATION_RESUME_UPLOADED", "PlacementApplication", applicationId, null);
    }

    @Test
    void attachResume_rejectsUploadForSomeoneElsesApplication() {
        User owner = User.builder().id(UUID.randomUUID()).build();
        PlacementApplication application = PlacementApplication.builder().id(applicationId).student(owner).build();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.attachResume(applicationId, file, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("your own application");
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void attachResume_rejectsWhenApplicationDoesNotExist() {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.attachResume(applicationId, file, studentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Application not found");
    }
}
