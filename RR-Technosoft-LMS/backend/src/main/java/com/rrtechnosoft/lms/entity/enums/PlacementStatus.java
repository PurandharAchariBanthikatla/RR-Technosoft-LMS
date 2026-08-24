package com.rrtechnosoft.lms.entity.enums;

/** Mirrors the Postgres `placement_status` enum (V1, extended in V8 with COMPLETED/CANCELLED). */
public enum PlacementStatus {
    DRAFT,
    OPEN,
    CLOSED,
    COMPLETED,
    CANCELLED
}
