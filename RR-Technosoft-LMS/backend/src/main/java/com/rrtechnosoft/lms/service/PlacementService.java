package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreatePlacementRequest;
import com.rrtechnosoft.lms.dto.request.UpdatePlacementRequest;
import com.rrtechnosoft.lms.dto.response.PlacementResponse;
import com.rrtechnosoft.lms.entity.Company;
import com.rrtechnosoft.lms.entity.Placement;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CompanyRepository;
import com.rrtechnosoft.lms.repository.IdCountProjection;
import com.rrtechnosoft.lms.repository.PlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Job Drives — CRUD over the `placements` table. See Placement entity javadoc for the table-name note. */
@Service
@RequiredArgsConstructor
public class PlacementService {

    private final PlacementRepository placementRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<PlacementResponse> list(String search, PlacementStatus status, UUID companyId, Pageable pageable) {
        Page<Placement> page = placementRepository.search(blankToNull(search), status, companyId, pageable);
        if (page.isEmpty()) {
            return page.map(p -> PlacementResponse.from(p, 0));
        }
        List<UUID> ids = page.getContent().stream().map(Placement::getId).toList();
        Map<UUID, Long> counts = toCountMap(placementRepository.countApplicationsByPlacementIds(ids));
        return page.map(p -> PlacementResponse.from(p, counts.getOrDefault(p.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public PlacementResponse get(UUID id) {
        Placement placement = findOrThrow(id);
        return PlacementResponse.from(placement, placementRepository.countApplications(id));
    }

    @Transactional
    public PlacementResponse create(CreatePlacementRequest request, UUID actorId) {
        Placement placement = Placement.builder()
                .roleTitle(request.role())
                .description(request.description())
                .eligibility(request.eligibility())
                .skillsRequired(request.skillsRequired() != null ? request.skillsRequired() : List.of())
                .allowedBranches(request.allowedBranches() != null ? request.allowedBranches() : List.of())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .minCgpa(request.minCgpa())
                .location(request.location())
                .jobType(request.jobType())
                .driveDate(request.driveDate())
                .lastDateToApply(request.lastDateToApply())
                .applicationLink(request.applicationLink())
                .status(PlacementStatus.OPEN)
                .postedBy(actorId)
                .build();
        applyCompanyLink(placement, request.companyId(), request.companyName(), request.companyLogoUrl());

        placement = placementRepository.save(placement);
        auditLogService.log(actorId, "PLACEMENT_CREATED", "Placement", placement.getId(), null);
        return PlacementResponse.from(placement, 0);
    }

    @Transactional
    public PlacementResponse update(UUID id, UpdatePlacementRequest request, UUID actorId) {
        Placement placement = findOrThrow(id);
        placement.setRoleTitle(request.role());
        placement.setDescription(request.description());
        placement.setEligibility(request.eligibility());
        placement.setSkillsRequired(request.skillsRequired() != null ? request.skillsRequired() : List.of());
        placement.setAllowedBranches(request.allowedBranches() != null ? request.allowedBranches() : List.of());
        placement.setSalaryMin(request.salaryMin());
        placement.setSalaryMax(request.salaryMax());
        placement.setMinCgpa(request.minCgpa());
        placement.setLocation(request.location());
        placement.setJobType(request.jobType());
        placement.setDriveDate(request.driveDate());
        placement.setLastDateToApply(request.lastDateToApply());
        placement.setApplicationLink(request.applicationLink());
        applyCompanyLink(placement, request.companyId(), request.companyName(), request.companyLogoUrl());

        placement = placementRepository.save(placement);
        auditLogService.log(actorId, "PLACEMENT_UPDATED", "Placement", id, null);
        return PlacementResponse.from(placement, placementRepository.countApplications(id));
    }

    @Transactional
    public PlacementResponse setStatus(UUID id, PlacementStatus status, UUID actorId) {
        Placement placement = findOrThrow(id);
        placement.setStatus(status);
        placement = placementRepository.save(placement);
        auditLogService.log(actorId, "PLACEMENT_STATUS_CHANGED", "Placement", id, null);
        return PlacementResponse.from(placement, placementRepository.countApplications(id));
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        Placement placement = findOrThrow(id);
        placementRepository.delete(placement);
        auditLogService.log(actorId, "PLACEMENT_DELETED", "Placement", id, null);
    }

    @Transactional(readOnly = true)
    public List<Placement> upcomingOpen(int limit) {
        return placementRepository.findUpcomingOpen(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    private void applyCompanyLink(Placement placement, UUID companyId, String companyName, String companyLogoUrl) {
        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> ApiException.notFound("Company not found"));
            placement.setCompany(company);
            placement.setCompanyName(company.getName());
            placement.setCompanyLogoUrl(company.getLogoUrl());
        } else {
            if (companyName == null || companyName.isBlank()) {
                throw ApiException.badRequest("companyId or companyName is required");
            }
            placement.setCompany(null);
            placement.setCompanyName(companyName);
            placement.setCompanyLogoUrl(companyLogoUrl);
        }
    }

    Placement findOrThrow(UUID id) {
        return placementRepository.findById(id).orElseThrow(() -> ApiException.notFound("Placement drive not found"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Map<UUID, Long> toCountMap(List<IdCountProjection> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (IdCountProjection row : rows) {
            map.put(row.getId(), row.getCnt());
        }
        return map;
    }
}
