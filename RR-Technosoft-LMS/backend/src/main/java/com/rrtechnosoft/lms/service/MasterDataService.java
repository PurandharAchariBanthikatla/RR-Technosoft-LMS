package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateMasterDataCategoryRequest;
import com.rrtechnosoft.lms.dto.request.CreateMasterDataItemRequest;
import com.rrtechnosoft.lms.dto.request.UpdateMasterDataCategoryRequest;
import com.rrtechnosoft.lms.dto.request.UpdateMasterDataItemRequest;
import com.rrtechnosoft.lms.dto.response.MasterDataCategoryResponse;
import com.rrtechnosoft.lms.dto.response.MasterDataItemResponse;
import com.rrtechnosoft.lms.entity.MasterDataCategory;
import com.rrtechnosoft.lms.entity.MasterDataItem;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.MasterDataCategoryRepository;
import com.rrtechnosoft.lms.repository.MasterDataItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.rrtechnosoft.lms.config.CacheConfig.MASTER_DATA_CATEGORIES;
import static com.rrtechnosoft.lms.config.CacheConfig.MASTER_DATA_ITEMS;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final MasterDataCategoryRepository categoryRepository;
    private final MasterDataItemRepository itemRepository;
    private final AuditLogService auditLogService;

    // ----- categories -----

    @Cacheable(cacheNames = MASTER_DATA_CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<MasterDataCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(MasterDataCategoryResponse::from).toList();
    }

    @CacheEvict(cacheNames = MASTER_DATA_CATEGORIES, allEntries = true)
    @Transactional
    public MasterDataCategoryResponse createCategory(CreateMasterDataCategoryRequest request, UUID actorId) {
        if (categoryRepository.existsByCode(request.code())) {
            throw ApiException.conflict("A master data category with code '" + request.code() + "' already exists");
        }
        MasterDataCategory category = MasterDataCategory.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .isSystem(false)
                .build();
        category = categoryRepository.save(category);
        auditLogService.log(actorId, "CREATE_MASTER_DATA_CATEGORY", "MasterDataCategory", category.getId(), null);
        return MasterDataCategoryResponse.from(category);
    }

    @CacheEvict(cacheNames = MASTER_DATA_CATEGORIES, allEntries = true)
    @Transactional
    public MasterDataCategoryResponse updateCategory(UUID id, UpdateMasterDataCategoryRequest request, UUID actorId) {
        MasterDataCategory category = findCategory(id);
        category.setName(request.name());
        category.setDescription(request.description());
        categoryRepository.save(category);
        auditLogService.log(actorId, "UPDATE_MASTER_DATA_CATEGORY", "MasterDataCategory", category.getId(), null);
        return MasterDataCategoryResponse.from(category);
    }

    @CacheEvict(cacheNames = MASTER_DATA_CATEGORIES, allEntries = true)
    @Transactional
    public void deleteCategory(UUID id, UUID actorId) {
        MasterDataCategory category = findCategory(id);
        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw ApiException.badRequest("System master data categories cannot be deleted");
        }
        categoryRepository.delete(category);
        auditLogService.log(actorId, "DELETE_MASTER_DATA_CATEGORY", "MasterDataCategory", id, null);
    }

    // ----- items -----

    @Cacheable(cacheNames = MASTER_DATA_ITEMS, key = "#categoryId + ':' + #activeOnly")
    @Transactional(readOnly = true)
    public List<MasterDataItemResponse> listItems(UUID categoryId, boolean activeOnly) {
        findCategory(categoryId); // 404 if category doesn't exist
        List<MasterDataItem> items = activeOnly
                ? itemRepository.findByCategoryIdAndIsActiveTrueOrderBySortOrderAscLabelAsc(categoryId)
                : itemRepository.findByCategoryIdOrderBySortOrderAscLabelAsc(categoryId);
        return items.stream().map(MasterDataItemResponse::from).toList();
    }

    @CacheEvict(cacheNames = MASTER_DATA_ITEMS, allEntries = true)
    @Transactional
    public MasterDataItemResponse createItem(UUID categoryId, CreateMasterDataItemRequest request, UUID actorId) {
        MasterDataCategory category = findCategory(categoryId);
        if (itemRepository.existsByCategoryIdAndCode(categoryId, request.code())) {
            throw ApiException.conflict("An item with code '" + request.code() + "' already exists in this category");
        }
        MasterDataItem item = MasterDataItem.builder()
                .category(category)
                .code(request.code())
                .label(request.label())
                .description(request.description())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .isActive(true)
                .metadata(request.metadata() != null ? request.metadata() : "{}")
                .createdBy(actorId)
                .build();
        item = itemRepository.save(item);
        auditLogService.log(actorId, "CREATE_MASTER_DATA_ITEM", "MasterDataItem", item.getId(), null);
        return MasterDataItemResponse.from(item);
    }

    @CacheEvict(cacheNames = MASTER_DATA_ITEMS, allEntries = true)
    @Transactional
    public MasterDataItemResponse updateItem(UUID itemId, UpdateMasterDataItemRequest request, UUID actorId) {
        MasterDataItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Master data item not found"));
        item.setLabel(request.label());
        item.setDescription(request.description());
        if (request.sortOrder() != null) item.setSortOrder(request.sortOrder());
        if (request.isActive() != null) item.setIsActive(request.isActive());
        if (request.metadata() != null) item.setMetadata(request.metadata());
        itemRepository.save(item);
        auditLogService.log(actorId, "UPDATE_MASTER_DATA_ITEM", "MasterDataItem", item.getId(), null);
        return MasterDataItemResponse.from(item);
    }

    @CacheEvict(cacheNames = MASTER_DATA_ITEMS, allEntries = true)
    @Transactional
    public void deleteItem(UUID itemId, UUID actorId) {
        MasterDataItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Master data item not found"));
        itemRepository.delete(item);
        auditLogService.log(actorId, "DELETE_MASTER_DATA_ITEM", "MasterDataItem", itemId, null);
    }

    private MasterDataCategory findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Master data category not found"));
    }
}
