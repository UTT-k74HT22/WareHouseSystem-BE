package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing inventory data.
 */
@Repository
public interface InventoryRepository extends
        JpaRepository<Inventory, String>,
        JpaSpecificationExecutor<Inventory> {
    /**
     * Find all inventories with pagination and filtering
     * @param spec Specification<Inventory>
     * @param pageable Pageable
     * @return Page<Inventory>
     */
    @Override
    @EntityGraph(attributePaths = {
            "product",
            "warehouse",
            "location",
            "batch"
    })
    Page<Inventory> findAll(Specification<Inventory> spec, Pageable pageable);
}