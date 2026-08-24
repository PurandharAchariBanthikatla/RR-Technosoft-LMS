package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.OrganizationProfile;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationProfileResponse(
        UUID id,
        String orgName,
        String legalName,
        String logoUrl,
        String faviconUrl,
        String website,
        String supportEmail,
        String supportPhone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String postalCode,
        String taxId,
        String timezone,
        String dateFormat,
        OffsetDateTime updatedAt
) {
    public static OrganizationProfileResponse from(OrganizationProfile o) {
        return new OrganizationProfileResponse(o.getId(), o.getOrgName(), o.getLegalName(), o.getLogoUrl(),
                o.getFaviconUrl(), o.getWebsite(), o.getSupportEmail(), o.getSupportPhone(), o.getAddressLine1(),
                o.getAddressLine2(), o.getCity(), o.getState(), o.getCountry(), o.getPostalCode(), o.getTaxId(),
                o.getTimezone(), o.getDateFormat(), o.getUpdatedAt());
    }
}
