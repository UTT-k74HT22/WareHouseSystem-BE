package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository interface for managing inventory data.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i FROM Inventory i
        WHERE i.productId = :productId
          AND i.warehouseId = :warehouseId
          AND ((:locationId IS NULL AND i.locationId IS NULL) OR i.locationId = :locationId)
          AND ((:batchId IS NULL AND i.batchId IS NULL) OR i.batchId = :batchId)
        """)
    Optional<Inventory> findByDimensionForUpdate(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId
    );
}
