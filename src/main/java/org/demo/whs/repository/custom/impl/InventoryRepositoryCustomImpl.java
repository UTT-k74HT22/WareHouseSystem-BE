package org.demo.whs.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.repository.custom.InventoryRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InventoryRepositoryCustomImpl implements InventoryRepositoryCustom {

    private final EntityManager entityManager;

    /**
     * Inventory summary for a product across all warehouses.
     */
    @Override
    public Optional<InventorySummaryResponse> getSummaryByProductId(String productId) {

        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    p.id AS productId,
                    p.sku AS productSku,
                    p.name AS productName,
                    COALESCE(SUM(i.on_hand_quantity),0) AS totalOnHandQuantity,
                    COALESCE(SUM(i.reserved_quantity),0) AS totalReservedQuantity,
                    COUNT(DISTINCT i.warehouse_id) AS warehouseCount,
                    COUNT(DISTINCT i.location_id) AS locationCount
                FROM products p
                LEFT JOIN inventory i ON p.id = i.product_id
                WHERE p.id = :productId
                GROUP BY p.id, p.sku, p.name
                """,
                "InventorySummaryResponseMapping"
        );

        query.setParameter("productId", productId);

        List<InventorySummaryResponse> result = query.getResultList();

        return result.stream().findFirst();
    }

    /**
     * Check stock availability.
     */
    @Override
    public CheckAvailabilityResponse getAvailability(
            String productId,
            String warehouseId,
            String locationId) {

        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    COALESCE(SUM(i.on_hand_quantity),0) AS totalOnHandQuantity,
                    COALESCE(SUM(i.reserved_quantity),0) AS totalReservedQuantity,
                    COALESCE(SUM(i.quarantine_quantity),0) AS totalQuarantineQuantity
                FROM inventory i
                WHERE i.product_id = :productId
                AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
                AND (:locationId IS NULL OR i.location_id = :locationId)
                """
        );

        query.setParameter("productId", productId);
        query.setParameter("warehouseId", warehouseId);
        query.setParameter("locationId", locationId);

        Object[] result = (Object[]) query.getSingleResult();

        BigDecimal onHand = (BigDecimal) result[0];
        BigDecimal reserved = (BigDecimal) result[1];
        BigDecimal quarantine = (BigDecimal) result[2];
        BigDecimal available = onHand.subtract(quarantine).subtract(reserved);

        return CheckAvailabilityResponse.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(locationId)
                .availableQuantity(available)
                .isAvailable(available.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    /**
     * Inventory grouped by location.
     */
    @Override
    public List<InventoryLocationProjection> getInventoryByLocation(InventoryFilterRequest filter) {

        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    l.id AS locationId,
                    l.code AS locationCode,
                    l.name AS locationName,
                    w.id AS warehouseId,
                    w.name AS warehouseName,
                    p.id AS productId,
                    p.sku AS productSku,
                    p.name AS productName,
                    b.id AS batchId,
                    b.batch_number AS batchNumber,
                    i.on_hand_quantity AS onHandQuantity,
                    i.quarantine_quantity AS quarantineQuantity,
                    i.reserved_quantity AS reservedQuantity
                FROM inventory i
                JOIN products p ON p.id = i.product_id
                LEFT JOIN locations l ON l.id = i.location_id
                JOIN warehouses w ON w.id = i.warehouse_id
                LEFT JOIN batches b ON b.id = i.batch_id
                WHERE (:productId IS NULL OR p.id = :productId)
                AND (:warehouseId IS NULL OR w.id = :warehouseId)
                AND (:locationId IS NULL OR l.id = :locationId)
                """,
                "InventoryByLocationResponseMapping"
        );

        query.setParameter("productId", filter.getProductId());
        query.setParameter("warehouseId", filter.getWarehouseId());
        query.setParameter("locationId", filter.getLocationId());

        return query.getResultList();
    }
}
