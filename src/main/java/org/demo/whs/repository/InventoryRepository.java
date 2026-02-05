package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing inventory data.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
}
