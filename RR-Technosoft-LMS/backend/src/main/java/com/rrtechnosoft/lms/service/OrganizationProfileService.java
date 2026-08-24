package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.UpdateOrganizationProfileRequest;
import com.rrtechnosoft.lms.dto.response.OrganizationProfileResponse;
import com.rrtechnosoft.lms.entity.OrganizationProfile;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.OrganizationProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationProfileService {

    private final OrganizationProfileRepository organizationProfileRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public OrganizationProfileResponse get() {
        return OrganizationProfileResponse.from(loadSingleton());
    }

    @Transactional
    public OrganizationProfileResponse update(UpdateOrganizationProfileRequest request, UUID actorId) {
        OrganizationProfile profile = loadSingleton();
        profile.setOrgName(request.orgName());
        profile.setLegalName(request.legalName());
        profile.setLogoUrl(request.logoUrl());
        profile.setFaviconUrl(request.faviconUrl());
        profile.setWebsite(request.website());
        profile.setSupportEmail(request.supportEmail());
        profile.setSupportPhone(request.supportPhone());
        profile.setAddressLine1(request.addressLine1());
        profile.setAddressLine2(request.addressLine2());
        profile.setCity(request.city());
        profile.setState(request.state());
        if (request.country() != null) profile.setCountry(request.country());
        profile.setPostalCode(request.postalCode());
        profile.setTaxId(request.taxId());
        if (request.timezone() != null) profile.setTimezone(request.timezone());
        if (request.dateFormat() != null) profile.setDateFormat(request.dateFormat());
        profile.setUpdatedBy(actorId);
        organizationProfileRepository.save(profile);
        auditLogService.log(actorId, "UPDATE_ORGANIZATION_PROFILE", "OrganizationProfile", profile.getId(), null);
        return OrganizationProfileResponse.from(profile);
    }

    private OrganizationProfile loadSingleton() {
        return organizationProfileRepository.findBySingletonGuardTrue()
                .orElseThrow(() -> ApiException.notFound("Organization profile not configured"));
    }
}
