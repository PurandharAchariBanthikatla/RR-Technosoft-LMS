package com.rrtechnosoft.lms.entity;

import com.rrtechnosoft.lms.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Identity is (course, student, attendanceDate) — see V4 migration.
 * liveClassId is an optional, unenforced link to the session a record
 * was taken in; it isn't mapped as an association since nothing reads
 * through it today (avoids a lazy fetch nobody needs).
 */
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "live_class_id")
    private UUID liveClassId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "marked_by")
    private UUID markedBy;

    @Column(name = "marked_at")
    @Builder.Default
    private OffsetDateTime markedAt = OffsetDateTime.now();
}
