package org.demo.whs.repository;

import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing stock adjustments data.
 */
@Repository
public interface StockAdjustmentsRepository extends JpaRepository<StockAdjustments, String> {

    /**
     * Finds a stock adjustment by its unique adjustment number.
     * @param adjustmentNumber the unique adjustment number to search for
     * @return an Optional containing the found StockAdjustments, or empty if not found
     */
    Optional<StockAdjustments> findByAdjustmentNumber(String adjustmentNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM StockAdjustments sa WHERE sa.id = :id")
    Optional<StockAdjustments> findByIdForUpdate(@Param("id") String id);

    /**
     * Checks if a stock adjustment exists with the given adjustment number.
     * @param adjustmentNumber the unique adjustment number to check for existence
     * @return true if a stock adjustment with the given adjustment number exists, false otherwise
     */
    boolean existsByAdjustmentNumber(String adjustmentNumber);

    /**
     * Retrieves a list of stock adjustments filtered by their status and ordered by creation date in ascending order.
     * @param status the status to filter stock adjustments by
     * @return a list of StockAdjustments matching the specified status, ordered by creation date ascending
     */
    List<StockAdjustments> findByStatusOrderByCreatedAtAsc(StockAdjustmentsStatus status);

    /**
     * Searches for stock adjustments based on multiple optional criteria, including status, product ID, warehouse ID,
     * inventory ID, adjustment number, and creation date range. The results are paginated according to the provided
     * Pageable object.
     *
     * @param status           the status to filter stock adjustments by (optional)
     * @param productId        the product ID to filter stock adjustments by (optional)
     * @param warehouseId      the warehouse ID to filter stock adjustments by (optional)
     * @param inventoryId      the inventory ID to filter stock adjustments by (optional)
     * @param adjustmentNumber the adjustment number to filter stock adjustments by (optional)
     * @param createdFrom      the start of the creation date range to filter stock adjustments by (optional)
     * @param createdTo        the end of the creation date range to filter stock adjustments by (optional)
     * @param pageable         the Pageable object containing pagination information
     * @return a paginated list of StockAdjustments matching the specified search criteria
     */
    @Query("""
        SELECT sa FROM StockAdjustments sa
        WHERE (:status IS NULL OR sa.status = :status)
          AND (:productId IS NULL OR sa.productId = :productId)
          AND (:warehouseId IS NULL OR sa.warehouseId = :warehouseId)
          AND (:inventoryId IS NULL OR sa.inventoryId = :inventoryId)
          AND (:adjustmentNumber IS NULL OR sa.adjustmentNumber = :adjustmentNumber)
          AND (:createdFrom IS NULL OR sa.createdAt >= :createdFrom)
          AND (:createdTo IS NULL OR sa.createdAt <= :createdTo)
        """)
    Page<StockAdjustments> search(
            @Param("status") StockAdjustmentsStatus status,
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("inventoryId") String inventoryId,
            @Param("adjustmentNumber") String adjustmentNumber,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );
}
