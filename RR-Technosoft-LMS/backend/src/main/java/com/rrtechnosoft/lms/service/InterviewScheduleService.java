package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateInterviewRequest;
import com.rrtechnosoft.lms.dto.request.UpdateInterviewRequest;
import com.rrtechnosoft.lms.dto.response.InterviewScheduleResponse;
import com.rrtechnosoft.lms.entity.InterviewSchedule;
import com.rrtechnosoft.lms.entity.PlacementApplication;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewScheduleService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final PlacementApplicationService placementApplicationService;
    private final AuditLogService auditLogService;

    @Transactional
    public InterviewScheduleResponse schedule(UUID applicationId, CreateInterviewRequest request, UUID actorId) {
        PlacementApplication application = placementApplicationService.findOrThrow(applicationId);

        InterviewSchedule interview = InterviewSchedule.builder()
                .application(application)
                .roundNumber(request.roundNumber() != null ? request.roundNumber() : nextRoundNumber(applicationId))
                .roundName(request.roundName())
                .scheduledAt(request.scheduledAt())
                .mode(request.mode())
                .venueOrLink(request.venueOrLink())
                .interviewerName(request.interviewerName())
                .createdBy(actorId)
                .build();
        interview = interviewScheduleRepository.save(interview);

        // Reflect that an interview is now in play on the parent application,
        // unless it's already past that stage (SELECTED/REJECTED/WITHDRAWN).
        if (application.getStatus() == ApplicationStatus.APPLIED || application.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        }

        auditLogService.log(actorId, "INTERVIEW_SCHEDULED", "InterviewSchedule", interview.getId(), null);
        return InterviewScheduleResponse.from(interview);
    }

    @Transactional
    public InterviewScheduleResponse update(UUID interviewId, UpdateInterviewRequest request, UUID actorId) {
        InterviewSchedule interview = findOrThrow(interviewId);
        interview.setRoundName(request.roundName());
        interview.setScheduledAt(request.scheduledAt());
        interview.setMode(request.mode());
        interview.setVenueOrLink(request.venueOrLink());
        interview.setInterviewerName(request.interviewerName());
        interview.setStatus(request.status());
        interview.setResult(request.result());
        interview.setFeedback(request.feedback());
        interview = interviewScheduleRepository.save(interview);

        auditLogService.log(actorId, "INTERVIEW_UPDATED", "InterviewSchedule", interviewId, null);
        return InterviewScheduleResponse.from(interview);
    }

    @Transactional
    public void delete(UUID interviewId, UUID actorId) {
        InterviewSchedule interview = findOrThrow(interviewId);
        interviewScheduleRepository.delete(interview);
        auditLogService.log(actorId, "INTERVIEW_DELETED", "InterviewSchedule", interviewId, null);
    }

    @Transactional(readOnly = true)
    public List<InterviewScheduleResponse> listForApplication(UUID applicationId) {
        return interviewScheduleRepository.findByApplication_IdOrderByRoundNumberAsc(applicationId)
                .stream().map(InterviewScheduleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewScheduleResponse> listForStudent(UUID studentId) {
        return interviewScheduleRepository.findByStudentId(studentId).stream().map(InterviewScheduleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewSchedule> upcoming(int windowDays) {
        OffsetDateTime now = OffsetDateTime.now();
        return interviewScheduleRepository.findScheduledBetween(now, now.plusDays(windowDays));
    }

    private int nextRoundNumber(UUID applicationId) {
        return interviewScheduleRepository.findByApplication_IdOrderByRoundNumberAsc(applicationId).size() + 1;
    }

    InterviewSchedule findOrThrow(UUID interviewId) {
        return interviewScheduleRepository.findById(interviewId)
                .orElseThrow(() -> ApiException.notFound("Interview not found"));
    }
}
