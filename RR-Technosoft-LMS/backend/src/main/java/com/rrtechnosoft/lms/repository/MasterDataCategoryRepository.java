package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.MasterDataCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasterDataCategoryRepository extends JpaRepository<MasterDataCategory, UUID> {
    List<MasterDataCategory> findAllByOrderByNameAsc();
    Optional<MasterDataCategory> findByCode(String code);
    boolean existsByCode(String code);
}
