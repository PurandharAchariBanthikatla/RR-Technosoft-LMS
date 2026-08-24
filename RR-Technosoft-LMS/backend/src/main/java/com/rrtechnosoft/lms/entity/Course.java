package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.CourseLevel;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * No @OneToMany collections here on purpose: modules/lessons/enrollments are
 * loaded and counted through their own repositories with batched aggregate
 * queries (see CourseRepository), not lazy collections on this entity. That
 * keeps course list/detail reads to a fixed, small number of queries
 * regardless of how many modules or enrollments a course has.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseLevel level = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "instructor_name", length = 150)
    private String instructorName;

    /** Reserved for the Faculty module; unused until then. */
    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /** Not writable via any API yet — reserved for a future reviews module. */
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
