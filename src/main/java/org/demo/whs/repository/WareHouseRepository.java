package org.demo.whs.repository;

import org.demo.whs.entity.Warehouses;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Warehouses entity.
 */
@Repository
public interface WareHouseRepository extends JpaRepository<Warehouses, String> {

    /**
     * Checks if a warehouse exists by its code.
     *
     * @param code the warehouse code
     * @return true if a warehouse with the given code exists, false otherwise
     */
    boolean existsByCode(String code);
}
