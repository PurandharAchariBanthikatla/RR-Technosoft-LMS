package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateLiveClassRequest;
import com.rrtechnosoft.lms.dto.response.LiveClassResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.LiveClass;
import com.rrtechnosoft.lms.entity.enums.LiveClassPlatform;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.LiveClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveClassServiceTest {

    @Mock private LiveClassRepository liveClassRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private LiveClassService liveClassService;

    private final UUID actorId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    @Test
    void create_rejectsEndTimeBeforeStartTime() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        var request = new CreateLiveClassRequest(courseId, "Recap session", LiveClassPlatform.ZOOM,
                "https://zoom.us/j/123", start, start.minusHours(1));

        assertThatThrownBy(() -> liveClassService.create(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void create_savesScheduledLiveClass() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(1);
        var request = new CreateLiveClassRequest(courseId, "Recap session", LiveClassPlatform.ZOOM,
                "https://zoom.us/j/123", start, end);

        Course course = Course.builder().id(courseId).title("SQL Bootcamp").build();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(liveClassRepository.save(any(LiveClass.class))).thenAnswer(inv -> {
            LiveClass lc = inv.getArgument(0);
            lc.setId(UUID.randomUUID());
            return lc;
        });

        LiveClassResponse response = liveClassService.create(request, actorId);

        assertThat(response.courseTitle()).isEqualTo("SQL Bootcamp");
        assertThat(response.meetingUrl()).isEqualTo("https://zoom.us/j/123");
    }
}
