package com.rrtechnosoft.lms.repository;

import java.util.UUID;

/**
 * Generic (id, count) row for batched GROUP BY aggregate queries — used to
 * build list-view counts (modules per course, lessons per module,
 * enrollments per course) in one query instead of one query per row.
 */
public interface IdCountProjection {
    UUID getId();
    Long getCnt();
}
