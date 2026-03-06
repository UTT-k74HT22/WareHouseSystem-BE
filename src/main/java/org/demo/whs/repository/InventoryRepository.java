package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.Optional;

/**
 * Repository interface for managing inventory data.
 */
@Repository
public interface InventoryRepository extends
        JpaRepository<Inventory, String>,
        JpaSpecificationExecutor<Inventory> {

    @Query("""
        SELECT 
            p.id as productId, 
            p.sku as productSku, 
            p.name as productName,
            SUM(i.onHandQuantity) as totalOnHandQuantity,
            SUM(i.reservedQuantity) as totalReservedQuantity,
            COUNT(DISTINCT i.warehouseId) as warehouseCount,
            COUNT(DISTINCT i.locationId) as locationCount
        FROM Products p
        LEFT JOIN Inventory i ON p.id = i.productId
        WHERE p.id = :productId
        GROUP BY p.id, p.sku, p.name
        """)
    Optional<InventorySummaryProjection> getSummaryByProductId(@Param("productId") String productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") String id);

    /**
     * Retrieves an inventory record by its dimensions with a pessimistic write lock to prevent concurrent modifications.
     *
     * @param productId   the ID of the product
     * @param warehouseId the ID of the warehouse
     * @param locationId  the ID of the location (nullable)
     * @param batchId     the ID of the batch (nullable)
     * @return an Optional containing the inventory record if found, or empty if not found
     */
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