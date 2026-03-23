package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.Inventory;
import org.demo.whs.repository.custom.InventoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends
        JpaRepository<Inventory, String>,
        JpaSpecificationExecutor<Inventory>,
        InventoryRepositoryCustom {

    /**
     * Lock inventory row by id for stock update operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.id = :id
    """)
    Optional<Inventory> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.productId = :productId
        AND i.warehouseId = :warehouseId
        AND (
            (:locationId IS NULL AND i.locationId IS NULL)
            OR (i.locationId = :locationId)
        )
        AND (
            (:batchId IS NULL AND i.batchId IS NULL)
            OR (i.batchId = :batchId)
        )
    """)
    Optional<Inventory> findByDimensionForUpdate(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId
    );

    /**
     * Find all inventory records matching non-null dimensions with pessimistic write lock.
     * If a dimension is null, it is not used as a filter.
     * Results are ordered by available quantity descending to pick the best candidate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.productId = :productId
        AND i.warehouseId = :warehouseId
        AND (:locationId IS NULL OR i.locationId = :locationId)
        AND (:batchId IS NULL OR i.batchId = :batchId)
        ORDER BY (i.onHandQuantity - i.quarantineQuantity - i.reservedQuantity) DESC
    """)
    List<Inventory> findAllSuitableForUpdate(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId
    );

    /**
     * Optimized: Find the single best inventory row that can fulfill the entire requested quantity.
     * This avoids scanning all rows in the application layer and uses DB-level filtering.
     * Note: @Lock is not supported for native queries, so we use "FOR UPDATE" in SQL.
     */
    @Query(value = """
        SELECT * FROM inventory i
        WHERE i.product_id = :productId
        AND i.warehouse_id = :warehouseId
        AND (:locationId IS NULL OR i.location_id = :locationId)
        AND (:batchId IS NULL OR i.batch_id = :batchId)
        AND (i.on_hand_quantity - COALESCE(i.quarantine_quantity, 0) - i.reserved_quantity) >= :requestedQuantity
        ORDER BY (i.on_hand_quantity - COALESCE(i.quarantine_quantity, 0) - i.reserved_quantity) DESC
        LIMIT 1
        FOR UPDATE
    """, nativeQuery = true)
    Optional<Inventory> findBestSuitableForUpdate(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId,
            @Param("requestedQuantity") java.math.BigDecimal requestedQuantity
    );

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Inventory i
        WHERE i.warehouseId = :warehouseId
            AND (i.onHandQuantity > 0 OR i.quarantineQuantity > 0 OR i.reservedQuantity > 0)
        """)
    boolean existsActiveInventoryByWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Inventory i
        WHERE i.locationId = :locationId
            AND (i.onHandQuantity > 0 OR i.quarantineQuantity > 0 OR i.reservedQuantity > 0)
        """)
    boolean existsActiveInventoryByLocationId(@Param("locationId") String locationId);

    /**
     * Find inventory by product, warehouse, location, and batch.
     * Used for validation or retrieval when all dimensions are specified.
     */
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.warehouseId = :warehouseId AND i.locationId = :locationId AND i.batchId = :batchId")
    Optional<Object> findByProductIdAndWarehouseIdAndLocationIdAndBatchId(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId
    );

    @Query("""
           SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
           FROM Inventory i
           WHERE i.batchId = :batchId
           AND i.reservedQuantity > 0
           """)
    boolean existsReservedStockByBatchId(@Param("batchId") String batchId);

    List<Inventory> findByBatchIdOrderByLastMovementAtDesc(String batchId);

    List<Inventory> findByBatchIdIn(Collection<String> batchIds);

    List<Inventory> findByWarehouseIdAndBatchIdIn(String warehouseId, Collection<String> batchIds);

    List<Inventory> findByProductIdAndBatchIdIn(String productId, Collection<String> batchIds);

    List<Inventory> findByProductIdAndWarehouseIdAndBatchIdIn(String productId, String warehouseId, Collection<String> batchIds);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Inventory i
        WHERE i.productId = :productId
            AND i.batchId IS NOT NULL
            AND (i.onHandQuantity > 0 OR i.quarantineQuantity > 0 OR i.reservedQuantity > 0)
        """)
    boolean existsBatchInventoryByProductId(@Param("productId") String productId);
}
