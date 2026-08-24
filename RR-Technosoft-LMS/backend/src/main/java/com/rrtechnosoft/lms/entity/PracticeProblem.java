package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.DifficultyLevel;
import com.rrtechnosoft.lms.entity.enums.PracticeTrack;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps the `practice_problems` table (present since V1__init_schema.sql) —
 * previously schema-only with no JPA entity, repository, service, or
 * controller. `test_cases` follows the same "raw JSON as text" approach as
 * QuizQuestion.options (see that class for the rationale): the shape only
 * matters wherever a problem is actually rendered, not at the entity level.
 */
@Entity
@Table(name = "practice_problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeProblem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PracticeTrack track;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_cases", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String testCases = "[]";

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 10;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
