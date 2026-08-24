package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.PracticeProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PracticeProblemRepository extends JpaRepository<PracticeProblem, UUID>,
        JpaSpecificationExecutor<PracticeProblem> {
}
