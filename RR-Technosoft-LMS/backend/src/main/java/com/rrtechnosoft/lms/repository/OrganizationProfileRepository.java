package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.OrganizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, UUID> {
    Optional<OrganizationProfile> findBySingletonGuardTrue();
}
