package com.rrtechnosoft.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateOrganizationProfileRequest(
        @NotBlank String orgName,
        String legalName,
        String logoUrl,
        String faviconUrl,
        String website,
        @Email String supportEmail,
        String supportPhone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String postalCode,
        String taxId,
        String timezone,
        String dateFormat
) {}
