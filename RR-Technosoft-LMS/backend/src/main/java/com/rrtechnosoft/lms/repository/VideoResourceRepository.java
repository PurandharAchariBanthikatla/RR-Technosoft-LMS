package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.VideoResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VideoResourceRepository extends JpaRepository<VideoResource, UUID> {

    @Query("""
        select v from VideoResource v
        where (:search is null or lower(v.title) like lower(concat('%', :search, '%')))
          and (:category is null or v.category = :category)
          and (:courseId is null or v.courseId = :courseId)
          and (:publishedOnly = false or v.isPublished = true)
        order by v.createdAt desc
        """)
    Page<VideoResource> search(@Param("search") String search,
                                @Param("category") String category,
                                @Param("courseId") UUID courseId,
                                @Param("publishedOnly") boolean publishedOnly,
                                Pageable pageable);

    long countByIsPublishedTrue();
}
