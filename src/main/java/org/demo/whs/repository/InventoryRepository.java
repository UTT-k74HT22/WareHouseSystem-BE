package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.demo.whs.repository.projection.InventoryAvailabilityProjection;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface InventoryRepository extends
        JpaRepository<Inventory, String>,
        JpaSpecificationExecutor<Inventory> {

    /**
     * Get inventory summary for a product across all warehouses/locations.
     */
    @Query("""
        SELECT
            p.id AS productId,
            p.sku AS productSku,
            p.name AS productName,
            COALESCE(SUM(i.onHandQuantity), 0) AS totalOnHandQuantity,
            COALESCE(SUM(i.reservedQuantity), 0) AS totalReservedQuantity,
            COUNT(DISTINCT i.warehouseId) AS warehouseCount,
            COUNT(DISTINCT i.locationId) AS locationCount
        FROM Products p
        LEFT JOIN Inventory i ON p.id = i.productId
        WHERE p.id = :productId
        GROUP BY p.id, p.sku, p.name
        """)
    Optional<InventorySummaryProjection> getSummaryByProductId(
            @Param("productId") String productId
    );

    /**
     * Get available inventory with optional warehouse/location filters.
     * Aggregation handled in database for performance.
     */
    @Query("""
        SELECT
            COALESCE(SUM(i.onHandQuantity), 0) AS totalOnHandQuantity,
            COALESCE(SUM(i.reservedQuantity), 0) AS totalReservedQuantity
        FROM Inventory i
        WHERE i.productId = :productId
        AND (:warehouseId IS NULL OR i.warehouseId = :warehouseId)
        AND (:locationId IS NULL OR i.locationId = :locationId)
        """)
    InventoryAvailabilityProjection getAvailability(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId
    );

    /**
     * Lock inventory row by id for stock update operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.id = :id
        """)
    Optional<Inventory> findByIdForUpdate(
            @Param("id") String id
    );

    /**
     * Lock inventory row by its dimensional keys.
     * Used for safe stock reservation or adjustment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.productId = :productId
        AND i.warehouseId = :warehouseId
        AND (
            (:locationId IS NULL AND i.locationId IS NULL)
            OR i.locationId = :locationId
        )
        AND (
            (:batchId IS NULL AND i.batchId IS NULL)
            OR i.batchId = :batchId
        )
        """)
    Optional<Inventory> findByDimensionForUpdate(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId
    );
}