package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateLiveClassRequest;
import com.rrtechnosoft.lms.dto.request.UpdateLiveClassRequest;
import com.rrtechnosoft.lms.dto.response.LiveClassResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.LiveClass;
import com.rrtechnosoft.lms.entity.enums.LiveClassStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.LiveClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<LiveClassResponse> list(LiveClassStatus status, UUID courseId, Pageable pageable) {
        return liveClassRepository.search(status, courseId, pageable).map(LiveClassResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LiveClassResponse> upcoming() {
        return liveClassRepository.findUpcoming(OffsetDateTime.now()).stream()
                .map(LiveClassResponse::from)
                .toList();
    }

    @Transactional
    public LiveClassResponse create(CreateLiveClassRequest request, UUID actorId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("endTime must be after startTime");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));

        LiveClass liveClass = LiveClass.builder()
                .course(course)
                .title(request.title())
                .platform(request.platform())
                .meetingLink(request.meetingLink())
                .scheduledStart(request.startTime())
                .scheduledEnd(request.endTime())
                .status(LiveClassStatus.SCHEDULED)
                .createdBy(actorId)
                .build();
        liveClass = liveClassRepository.save(liveClass);
        auditLogService.log(actorId, "CREATE_LIVE_CLASS", "LiveClass", liveClass.getId(), null);
        return LiveClassResponse.from(liveClass);
    }

    @Transactional
    public LiveClassResponse update(UUID id, UpdateLiveClassRequest request, UUID actorId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("endTime must be after startTime");
        }
        LiveClass liveClass = liveClassRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Live class not found"));

        liveClass.setTitle(request.title());
        liveClass.setPlatform(request.platform());
        liveClass.setMeetingLink(request.meetingLink());
        liveClass.setScheduledStart(request.startTime());
        liveClass.setScheduledEnd(request.endTime());
        liveClass.setStatus(request.status());
        liveClass.setRecordingUrl(request.recordingUrl());
        liveClass = liveClassRepository.save(liveClass);
        auditLogService.log(actorId, "UPDATE_LIVE_CLASS", "LiveClass", id, null);
        return LiveClassResponse.from(liveClass);
    }
}
