package org.demo.whs.repository;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Category entity operations.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    Page<Category> findAllByStatus(CategoryStatus status, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE "
            + "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND "
            + "(:status IS NULL OR c.status = :status)")
    Page<Category> search(@Param("keyword") String keyword,
                          @Param("status") CategoryStatus status,
                          Pageable pageable);
}
