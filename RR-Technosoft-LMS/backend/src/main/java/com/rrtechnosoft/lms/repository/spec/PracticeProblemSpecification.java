package com.rrtechnosoft.lms.repository.spec;

import com.rrtechnosoft.lms.entity.PracticeProblem;
import com.rrtechnosoft.lms.entity.enums.DifficultyLevel;
import com.rrtechnosoft.lms.entity.enums.PracticeTrack;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filters for GET /practice/problems — every parameter is optional. */
public final class PracticeProblemSpecification {

    private PracticeProblemSpecification() {
    }

    public static Specification<PracticeProblem> filter(String search, DifficultyLevel difficulty, PracticeTrack track) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (search != null && !search.isBlank()) {
                predicates = cb.and(predicates, cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (difficulty != null) {
                predicates = cb.and(predicates, cb.equal(root.get("difficulty"), difficulty));
            }
            if (track != null) {
                predicates = cb.and(predicates, cb.equal(root.get("track"), track));
            }

            return predicates;
        };
    }
}
