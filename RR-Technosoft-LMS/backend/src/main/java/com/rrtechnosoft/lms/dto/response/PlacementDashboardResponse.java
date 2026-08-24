package com.rrtechnosoft.lms.dto.response;

import java.util.List;
import java.util.Map;

public record PlacementDashboardResponse(
        long totalCompanies,
        long activeCompanies,
        long totalDrives,
        long openDrives,
        long totalApplications,
        long selectedCount,
        long shortlistedCount,
        long rejectedCount,
        double placementRate,
        long upcomingInterviewsCount,
        Map<String, Long> applicationsByStatus,
        List<PlacementResponse> upcomingDrives,
        List<InterviewScheduleResponse> upcomingInterviews
) {}
