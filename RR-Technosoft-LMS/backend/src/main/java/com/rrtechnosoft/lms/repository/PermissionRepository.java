package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findAllByOrderByCategoryAscNameAsc();
    Optional<Permission> findByCode(String code);
    boolean existsByCode(String code);
}
