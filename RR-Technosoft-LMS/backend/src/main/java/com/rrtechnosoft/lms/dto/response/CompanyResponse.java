package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Company;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String logoUrl,
        String website,
        String industry,
        String description,
        String contactPersonName,
        String contactEmail,
        String contactPhone,
        String address,
        Boolean isActive,
        long activeDriveCount,
        OffsetDateTime createdAt
) {
    public static CompanyResponse from(Company c, long activeDriveCount) {
        return new CompanyResponse(
                c.getId(), c.getName(), c.getLogoUrl(), c.getWebsite(), c.getIndustry(), c.getDescription(),
                c.getContactPersonName(), c.getContactEmail(), c.getContactPhone(), c.getAddress(),
                c.getIsActive(), activeDriveCount, c.getCreatedAt()
        );
    }
}
