package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.MasterDataItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasterDataItemRepository extends JpaRepository<MasterDataItem, UUID> {
    List<MasterDataItem> findByCategoryIdOrderBySortOrderAscLabelAsc(UUID categoryId);
    List<MasterDataItem> findByCategoryIdAndIsActiveTrueOrderBySortOrderAscLabelAsc(UUID categoryId);
    List<MasterDataItem> findByCategory_CodeAndIsActiveTrueOrderBySortOrderAscLabelAsc(String categoryCode);
    Optional<MasterDataItem> findByCategoryIdAndCode(UUID categoryId, String code);
    boolean existsByCategoryIdAndCode(UUID categoryId, String code);
}
