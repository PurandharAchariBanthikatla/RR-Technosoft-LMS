package com.rrtechnosoft.lms.repository.spec;

import com.rrtechnosoft.lms.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Builds a dynamic {@link Specification} for the searchable audit log history
 * (GET /administration/audit-logs). Every filter is optional and independent —
 * only non-null criteria are applied, so callers can search by any combination
 * of actor, action, entity type/id, and a created-at date range.
 */
public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> filter(UUID actorId,
                                                   String action,
                                                   String entityType,
                                                   UUID entityId,
                                                   OffsetDateTime from,
                                                   OffsetDateTime to) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (actorId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("actorId"), actorId));
            }
            if (action != null && !action.isBlank()) {
                predicates = cb.and(predicates, cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates = cb.and(predicates, cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase()));
            }
            if (entityId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return predicates;
        };
    }
}
