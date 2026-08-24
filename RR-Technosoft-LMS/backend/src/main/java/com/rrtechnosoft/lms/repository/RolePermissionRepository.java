package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.RolePermission;
import com.rrtechnosoft.lms.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findAllByOrderByRoleAsc();

    List<RolePermission> findByRole(UserRole role);

    Optional<RolePermission> findByRoleAndPermissionId(UserRole role, UUID permissionId);

    boolean existsByRoleAndPermissionIdAndAllowedTrue(UserRole role, UUID permissionId);

    boolean existsByRoleAndPermission_CodeAndAllowedTrue(UserRole role, String code);
}
