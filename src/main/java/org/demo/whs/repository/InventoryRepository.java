package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}