package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateCompanyRequest;
import com.rrtechnosoft.lms.dto.request.UpdateCompanyRequest;
import com.rrtechnosoft.lms.dto.response.CompanyResponse;
import com.rrtechnosoft.lms.entity.Company;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CompanyRepository;
import com.rrtechnosoft.lms.repository.PlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PlacementRepository placementRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(String search, Boolean isActive, Pageable pageable) {
        return companyRepository.search(blankToNull(search), isActive, pageable)
                .map(c -> CompanyResponse.from(c, activeDriveCount(c.getId())));
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(UUID id) {
        Company company = findOrThrow(id);
        return CompanyResponse.from(company, activeDriveCount(id));
    }

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, UUID actorId) {
        if (companyRepository.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("A company with this name already exists");
        }
        Company company = Company.builder()
                .name(request.name())
                .logoUrl(request.logoUrl())
                .website(request.website())
                .industry(request.industry())
                .description(request.description())
                .contactPersonName(request.contactPersonName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .address(request.address())
                .isActive(true)
                .createdBy(actorId)
                .build();
        company = companyRepository.save(company);
        auditLogService.log(actorId, "COMPANY_CREATED", "Company", company.getId(), null);
        return CompanyResponse.from(company, 0);
    }

    @Transactional
    public CompanyResponse update(UUID id, UpdateCompanyRequest request, UUID actorId) {
        Company company = findOrThrow(id);
        if (companyRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw ApiException.conflict("A company with this name already exists");
        }
        company.setName(request.name());
        company.setLogoUrl(request.logoUrl());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setDescription(request.description());
        company.setContactPersonName(request.contactPersonName());
        company.setContactEmail(request.contactEmail());
        company.setContactPhone(request.contactPhone());
        company.setAddress(request.address());
        if (request.isActive() != null) {
            company.setIsActive(request.isActive());
        }
        company = companyRepository.save(company);
        auditLogService.log(actorId, "COMPANY_UPDATED", "Company", id, null);
        return CompanyResponse.from(company, activeDriveCount(id));
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        Company company = findOrThrow(id);
        // Soft-delete: a company with posted drives can't be hard-deleted without
        // orphaning placements.company_id history, so deactivate instead.
        company.setIsActive(false);
        companyRepository.save(company);
        auditLogService.log(actorId, "COMPANY_DEACTIVATED", "Company", id, null);
    }

    private long activeDriveCount(UUID companyId) {
        return placementRepository.search(null, PlacementStatus.OPEN, companyId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
    }

    private Company findOrThrow(UUID id) {
        return companyRepository.findById(id).orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
