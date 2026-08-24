package com.rrtechnosoft.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String department;

    private String designation;

    /** JSON array of fine-grained permission scopes, e.g. ["COURSE_MANAGE","PLACEMENT_POST"] */
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String permissions = "[]";

    @Column(name = "assigned_by")
    private UUID assignedBy;
}
