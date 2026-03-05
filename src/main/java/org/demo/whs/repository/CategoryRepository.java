package org.demo.whs.repository;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Category entity operations.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    Page<Category> findAllByStatus(CategoryStatus status, Pageable pageable);
}
