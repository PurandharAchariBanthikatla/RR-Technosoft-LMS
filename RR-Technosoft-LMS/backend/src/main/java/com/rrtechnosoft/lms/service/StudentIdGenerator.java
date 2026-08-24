package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Generates IDs like RRT2026S0001. Not a DB sequence on purpose — students
 * are created in bursts by admins uploading batches, and a human-readable,
 * year-scoped ID is easier to support students with than a raw counter.
 */
@Component
@RequiredArgsConstructor
public class StudentIdGenerator {

    private final UserRepository userRepository;

    public String next() {
        int year = Year.now().getValue();
        String prefix = "RRT" + year + "S";
        long countThisYear = userRepository.countByStudentIdStartingWith(prefix);
        String candidate;
        long seq = countThisYear + 1;
        do {
            candidate = prefix + String.format("%04d", seq);
            seq++;
        } while (userRepository.existsByStudentId(candidate));
        return candidate;
    }
}
