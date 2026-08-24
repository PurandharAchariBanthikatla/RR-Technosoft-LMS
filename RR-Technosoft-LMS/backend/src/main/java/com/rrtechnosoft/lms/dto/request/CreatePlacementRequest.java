package com.rrtechnosoft.lms.dto.request;

import com.rrtechnosoft.lms.entity.enums.JobType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePlacementRequest(
        UUID companyId,
        /** Required when companyId is null — lets an admin post a drive without pre-registering the company. */
        @Size(max = 200) String companyName,
        String companyLogoUrl,
        @NotBlank @Size(min = 2, max = 200) String role,
        String description,
        String eligibility,
        List<String> skillsRequired,
        List<String> allowedBranches,
        @DecimalMin(value = "0") BigDecimal salaryMin,
        @DecimalMin(value = "0") BigDecimal salaryMax,
        @DecimalMin(value = "0") @DecimalMax(value = "10") BigDecimal minCgpa,
        @Size(max = 150) String location,
        @NotNull JobType jobType,
        LocalDate driveDate,
        @NotNull LocalDate lastDateToApply,
        String applicationLink
) {}
