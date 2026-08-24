package com.rrtechnosoft.lms.dto.response;

import com.rrtechnosoft.lms.entity.Placement;
import com.rrtechnosoft.lms.entity.enums.JobType;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Field names companyName/role/location/packageLpa/eligibility/driveDate/
 * status/applicantsCount are pinned to the frontend `PlacementDrive` type
 * (src/types/index.ts) — do not rename. Everything else is additive, for
 * the richer admin Job Drive screens.
 */
public record PlacementResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String companyLogoUrl,
        String role,
        String description,
        String eligibility,
        List<String> skillsRequired,
        List<String> allowedBranches,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String packageLpa,
        BigDecimal minCgpa,
        String location,
        JobType jobType,
        LocalDate driveDate,
        LocalDate lastDateToApply,
        String applicationLink,
        PlacementStatus status,
        long applicantsCount,
        OffsetDateTime createdAt
) {
    public static PlacementResponse from(Placement p, long applicantsCount) {
        return new PlacementResponse(
                p.getId(),
                p.getCompany() != null ? p.getCompany().getId() : null,
                p.getCompanyName(), p.getCompanyLogoUrl(), p.getRoleTitle(), p.getDescription(), p.getEligibility(),
                p.getSkillsRequired(), p.getAllowedBranches(), p.getSalaryMin(), p.getSalaryMax(),
                formatPackage(p.getSalaryMin(), p.getSalaryMax()), p.getMinCgpa(), p.getLocation(), p.getJobType(),
                p.getDriveDate(), p.getLastDateToApply(), p.getApplicationLink(), p.getStatus(), applicantsCount,
                p.getCreatedAt()
        );
    }

    private static String formatPackage(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        if (min != null && max != null && min.compareTo(max) != 0) {
            return stripZeros(min) + " - " + stripZeros(max) + " LPA";
        }
        BigDecimal single = min != null ? min : max;
        return stripZeros(single) + " LPA";
    }

    private static String stripZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
