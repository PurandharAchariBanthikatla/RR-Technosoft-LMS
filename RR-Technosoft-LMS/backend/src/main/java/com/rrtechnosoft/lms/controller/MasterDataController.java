package com.rrtechnosoft.lms.controller;

import com.rrtechnosoft.lms.dto.request.CreateMasterDataCategoryRequest;
import com.rrtechnosoft.lms.dto.request.CreateMasterDataItemRequest;
import com.rrtechnosoft.lms.dto.request.UpdateMasterDataCategoryRequest;
import com.rrtechnosoft.lms.dto.request.UpdateMasterDataItemRequest;
import com.rrtechnosoft.lms.dto.response.MasterDataCategoryResponse;
import com.rrtechnosoft.lms.dto.response.MasterDataItemResponse;
import com.rrtechnosoft.lms.security.UserPrincipal;
import com.rrtechnosoft.lms.service.MasterDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GET is open to any authenticated user (SecurityConfig) — every module's
 * dropdowns (departments, skill tags, course categories...) read from here;
 * writes are SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/administration/master-data")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/categories")
    public ResponseEntity<List<MasterDataCategoryResponse>> listCategories() {
        return ResponseEntity.ok(masterDataService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterDataCategoryResponse> createCategory(@Valid @RequestBody CreateMasterDataCategoryRequest request,
                                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createCategory(request, principal.getId()));
    }

    @PatchMapping("/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterDataCategoryResponse> updateCategory(@PathVariable UUID id,
                                                                       @Valid @RequestBody UpdateMasterDataCategoryRequest request,
                                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterDataService.updateCategory(id, request, principal.getId()));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        masterDataService.deleteCategory(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories/{categoryId}/items")
    public ResponseEntity<List<MasterDataItemResponse>> listItems(@PathVariable UUID categoryId,
                                                                    @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(masterDataService.listItems(categoryId, activeOnly));
    }

    @PostMapping("/categories/{categoryId}/items")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterDataItemResponse> createItem(@PathVariable UUID categoryId,
                                                               @Valid @RequestBody CreateMasterDataItemRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createItem(categoryId, request, principal.getId()));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterDataItemResponse> updateItem(@PathVariable UUID itemId,
                                                               @Valid @RequestBody UpdateMasterDataItemRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterDataService.updateItem(itemId, request, principal.getId()));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId, @AuthenticationPrincipal UserPrincipal principal) {
        masterDataService.deleteItem(itemId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
