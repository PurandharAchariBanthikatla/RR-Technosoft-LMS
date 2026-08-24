package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.JobType;
import com.rrtechnosoft.lms.entity.enums.PlacementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Backs the pre-existing `placements` table (V1) — this IS the "Job Drive"
 * entity the Placement module's Job Drives feature manages. Column/table
 * name stayed `placements` on purpose to avoid a breaking rename of a table
 * already in use; company_name/company_logo_url are a denormalized snapshot
 * kept in sync with `company` (via company_id) by PlacementService whenever
 * a managed company is linked, so existing consumers reading those two
 * columns directly are unaffected.
 */
@Entity
@Table(name = "placements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Placement {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_logo_url", columnDefinition = "TEXT")
    private String companyLogoUrl;

    @Column(name = "role_title", nullable = false, length = 200)
    private String roleTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills_required", nullable = false)
    @Builder.Default
    private List<String> skillsRequired = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_branches", nullable = false)
    @Builder.Default
    private List<String> allowedBranches = List.of();

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "min_cgpa", precision = 3, scale = 2)
    private BigDecimal minCgpa;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    @Builder.Default
    private JobType jobType = JobType.FULL_TIME;

    @Column(name = "drive_date")
    private LocalDate driveDate;

    @Column(name = "last_date_to_apply", nullable = false)
    private LocalDate lastDateToApply;

    @Column(name = "application_link", columnDefinition = "TEXT")
    private String applicationLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlacementStatus status = PlacementStatus.OPEN;

    @Column(name = "posted_by", nullable = false)
    private UUID postedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
