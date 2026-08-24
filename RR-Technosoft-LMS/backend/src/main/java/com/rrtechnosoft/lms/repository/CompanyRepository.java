package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @Query("""
        select c from Company c
        where (:search is null or lower(c.name) like lower(concat('%', :search, '%')))
          and (:isActive is null or c.isActive = :isActive)
        order by c.name asc
        """)
    Page<Company> search(@Param("search") String search, @Param("isActive") Boolean isActive, Pageable pageable);

    long countByIsActiveTrue();
}
