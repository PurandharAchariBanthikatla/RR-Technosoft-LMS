package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.response.InterviewScheduleResponse;
import com.rrtechnosoft.lms.dto.response.PlacementDashboardResponse;
import com.rrtechnosoft.lms.dto.response.PlacementResponse;
import com.rrtechnosoft.lms.entity.enums.ApplicationStatus;
import com.rrtechnosoft.lms.entity.enums.InterviewStatus;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import com.rrtechnosoft.lms.repository.CompanyRepository;
import com.rrtechnosoft.lms.repository.InterviewScheduleRepository;
import com.rrtechnosoft.lms.repository.PlacementApplicationRepository;
import com.rrtechnosoft.lms.repository.PlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlacementDashboardService {

    private final CompanyRepository companyRepository;
    private final PlacementRepository placementRepository;
    private final PlacementApplicationRepository applicationRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;

    @Transactional(readOnly = true)
    public PlacementDashboardResponse summary() {
        long totalCompanies = companyRepository.count();
        long activeCompanies = companyRepository.countByIsActiveTrue();
        long totalDrives = placementRepository.count();
        long openDrives = placementRepository.countByStatus(PlacementStatus.OPEN);
        long totalApplications = applicationRepository.count();
        long selected = applicationRepository.countByStatus(ApplicationStatus.SELECTED);
        long shortlisted = applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED);
        long rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        double placementRate = totalApplications == 0 ? 0.0 : (selected * 100.0) / totalApplications;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status.name(), applicationRepository.countByStatus(status));
        }

        List<PlacementResponse> upcomingDrives = placementRepository.findUpcomingOpen(PageRequest.of(0, 5))
                .stream()
                .map(p -> PlacementResponse.from(p, placementRepository.countApplications(p.getId())))
                .toList();

        List<InterviewScheduleResponse> upcomingInterviews = interviewScheduleRepository
                .findScheduledBetween(java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusDays(14))
                .stream().map(InterviewScheduleResponse::from).toList();

        long upcomingInterviewsCount = interviewScheduleRepository.countByStatus(InterviewStatus.SCHEDULED);

        return new PlacementDashboardResponse(
                totalCompanies, activeCompanies, totalDrives, openDrives, totalApplications,
                selected, shortlisted, rejected, placementRate, upcomingInterviewsCount,
                byStatus, upcomingDrives, upcomingInterviews
        );
    }
}
