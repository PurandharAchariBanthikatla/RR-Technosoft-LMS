package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, UUID> {
}
