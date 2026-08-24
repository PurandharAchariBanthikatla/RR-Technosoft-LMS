package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.LearningResource;
import com.rrtechnosoft.lms.entity.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LearningResourceRepository extends JpaRepository<LearningResource, UUID> {

    @Query("""
        select r from LearningResource r
        where (:search is null or lower(r.title) like lower(concat('%', :search, '%')))
          and (:category is null or r.category = :category)
          and (:resourceType is null or r.resourceType = :resourceType)
          and (:courseId is null or r.courseId = :courseId)
          and (:publishedOnly = false or r.isPublished = true)
        order by r.createdAt desc
        """)
    Page<LearningResource> search(@Param("search") String search,
                                   @Param("category") String category,
                                   @Param("resourceType") ResourceType resourceType,
                                   @Param("courseId") UUID courseId,
                                   @Param("publishedOnly") boolean publishedOnly,
                                   Pageable pageable);

    long countByIsPublishedTrue();
}
