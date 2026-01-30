package org.demo.whs.repository;

import org.demo.whs.entity.Warehouses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Warehouses entity.
 */
@Repository
public interface WareHouseRepository extends JpaRepository<Warehouses, String> {
}
