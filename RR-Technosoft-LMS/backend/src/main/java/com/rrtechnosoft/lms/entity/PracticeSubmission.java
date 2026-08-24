package com.rrtechnosoft.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps the `practice_submissions` table (present since V1__init_schema.sql).
 * `isCorrect`/`runtimeMs` are populated by an automated grader in principle,
 * but this project has no sandboxed code-execution engine (building one
 * safely — isolated, resource-limited, multi-language — is a substantial,
 * security-critical undertaking of its own and is out of scope here). Until
 * one exists, submissions are recorded as-is with isCorrect defaulted to
 * false; the frontend practice problem page already reflects this (it shows
 * "Submission received", not a pass/fail verdict — see PracticeProblemPage).
 */
@Entity
@Table(name = "practice_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSubmission {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private PracticeProblem problem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false, length = 30)
    private String language;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private OffsetDateTime submittedAt = OffsetDateTime.now();
}
