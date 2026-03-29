package org.demo.whs.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.response.Dashboard.DashboardActivityResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardOverviewResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardTrendPointResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardWarehouseCapacityResponse;
import org.demo.whs.repository.custom.DashboardRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardRepositoryCustomImpl implements DashboardRepositoryCustom {

    private static final int CAPACITY_WARNING_PERCENT = 80;

    private final EntityManager entityManager;

    @Override
    public DashboardOverviewResponse getOverview(String warehouseId) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    COALESCE(SUM(i.on_hand_quantity), 0) AS totalOnHandQuantity,
                    COALESCE(SUM(i.reserved_quantity), 0) AS totalReservedQuantity,
                    COALESCE(SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity), 0) AS totalAvailableQuantity,
                    (
                        SELECT COUNT(*)
                        FROM (
                            SELECT p.id
                            FROM products p
                            LEFT JOIN inventory inv ON inv.product_id = p.id
                            WHERE (:warehouseId IS NULL OR inv.warehouse_id = :warehouseId)
                            GROUP BY p.id, p.reorder_point, p.min_stock_level
                            HAVING COALESCE(SUM(inv.on_hand_quantity - inv.reserved_quantity - inv.quarantine_quantity), 0)
                                   <= COALESCE(NULLIF(p.reorder_point, 0), NULLIF(p.min_stock_level, 0), 0)
                                   AND COALESCE(NULLIF(p.reorder_point, 0), NULLIF(p.min_stock_level, 0), 0) > 0
                        ) low_stock
                    ) AS lowStockSkuCount,
                    (
                        SELECT COUNT(DISTINCT b.id)
                        FROM batches b
                        JOIN inventory bi ON bi.batch_id = b.id
                        WHERE b.expiry_date IS NOT NULL
                          AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)
                          AND COALESCE(bi.on_hand_quantity, 0) > 0
                          AND (:warehouseId IS NULL OR bi.warehouse_id = :warehouseId)
                    ) AS expiringBatchCount,
                    (
                        SELECT COUNT(*)
                        FROM warehouses w
                        WHERE w.status = 'ACTIVE'
                    ) AS activeWarehouseCount,
                    (
                        SELECT COUNT(*)
                        FROM (
                            SELECT
                                w.id,
                                CASE
                                    WHEN COUNT(DISTINCT CASE WHEN l.status <> 'INACTIVE' THEN l.id END) = 0 THEN 0
                                    ELSE (
                                        COUNT(DISTINCT CASE
                                            WHEN l.status <> 'INACTIVE' AND inv_loc.on_hand_quantity > 0 THEN l.id
                                        END) * 100.0
                                    ) / COUNT(DISTINCT CASE WHEN l.status <> 'INACTIVE' THEN l.id END)
                                END AS utilizationPercent
                            FROM warehouses w
                            LEFT JOIN locations l ON l.warehouse_id = w.id
                            LEFT JOIN inventory inv_loc ON inv_loc.location_id = l.id
                            WHERE w.status = 'ACTIVE'
                              AND (:warehouseId IS NULL OR w.id = :warehouseId)
                            GROUP BY w.id
                        ) capacity_snapshot
                        WHERE utilizationPercent >= :capacityWarningPercent
                    ) AS nearCapacityWarehouseCount,
                    (
                        SELECT COUNT(*)
                        FROM inbound_receipts ir
                        WHERE ir.status IN ('DRAFT', 'CONFIRMED')
                          AND (:warehouseId IS NULL OR ir.warehouse_id = :warehouseId)
                    ) AS pendingInboundReceipts,
                    (
                        SELECT COUNT(*)
                        FROM outbound_shipments os
                        WHERE os.status IN ('DRAFT', 'PICKING', 'PACKED')
                          AND (:warehouseId IS NULL OR os.warehouse_id = :warehouseId)
                    ) AS pendingOutboundShipments,
                    (
                        SELECT COUNT(*)
                        FROM purchase_orders po
                        WHERE po.status IN ('APPROVED', 'CONFIRMED', 'PARTIALLY_RECEIVED')
                          AND (:warehouseId IS NULL OR po.warehouse_id = :warehouseId)
                    ) AS openPurchaseOrders,
                    (
                        SELECT COUNT(*)
                        FROM sales_orders so
                        WHERE so.status IN ('CONFIRMED', 'PARTIALLY_SHIPPED')
                          AND (:warehouseId IS NULL OR so.warehouse_id = :warehouseId)
                    ) AS openSalesOrders,
                    (
                        SELECT COUNT(*)
                        FROM background_jobs bj
                        WHERE bj.status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'GENERATING_FILE', 'UPLOADING')
                    ) AS runningJobs,
                    (
                        SELECT COUNT(*)
                        FROM background_jobs bj
                        WHERE bj.status = 'FAILED'
                          AND DATE(bj.updated_at) = CURDATE()
                    ) AS failedJobsToday
                FROM inventory i
                WHERE (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
                """
        );

        query.setParameter("warehouseId", warehouseId);
        query.setParameter("capacityWarningPercent", CAPACITY_WARNING_PERCENT);

        Object[] row = (Object[]) query.getSingleResult();

        return DashboardOverviewResponse.builder()
                .totalOnHandQuantity(getBigDecimal(row[0]))
                .totalReservedQuantity(getBigDecimal(row[1]))
                .totalAvailableQuantity(getBigDecimal(row[2]))
                .lowStockSkuCount(getInteger(row[3]))
                .expiringBatchCount(getInteger(row[4]))
                .activeWarehouseCount(getInteger(row[5]))
                .nearCapacityWarehouseCount(getInteger(row[6]))
                .pendingInboundReceipts(getInteger(row[7]))
                .pendingOutboundShipments(getInteger(row[8]))
                .openPurchaseOrders(getInteger(row[9]))
                .openSalesOrders(getInteger(row[10]))
                .runningJobs(getInteger(row[11]))
                .failedJobsToday(getInteger(row[12]))
                .build();
    }

    @Override
    public List<DashboardTrendPointResponse> getMovementTrend(String warehouseId, int days) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    DATE(sm.movement_date) AS trendDate,
                    SUM(CASE WHEN sm.movement_type = 'INBOUND' THEN 1 ELSE 0 END) AS inboundCount,
                    SUM(CASE WHEN sm.movement_type = 'OUTBOUND' THEN 1 ELSE 0 END) AS outboundCount
                FROM stock_movements sm
                WHERE sm.movement_date >= DATE_SUB(CURDATE(), INTERVAL :days DAY)
                  AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                GROUP BY DATE(sm.movement_date)
                ORDER BY trendDate ASC
                """
        );

        query.setParameter("days", days);
        query.setParameter("warehouseId", warehouseId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> DashboardTrendPointResponse.builder()
                        .label(row[0] != null ? row[0].toString() : null)
                        .inboundCount(getLong(row[1]))
                        .outboundCount(getLong(row[2]))
                        .build())
                .toList();
    }

    @Override
    public List<DashboardWarehouseCapacityResponse> getWarehouseCapacities(String warehouseId, int limit) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    w.id AS warehouseId,
                    w.name AS warehouseName,
                    COUNT(DISTINCT CASE
                        WHEN l.status <> 'INACTIVE' AND inv.on_hand_quantity > 0 THEN l.id
                    END) AS occupiedLocations,
                    COUNT(DISTINCT CASE
                        WHEN l.status <> 'INACTIVE' THEN l.id
                    END) AS totalLocations
                FROM warehouses w
                LEFT JOIN locations l ON l.warehouse_id = w.id
                LEFT JOIN inventory inv ON inv.location_id = l.id
                WHERE w.status = 'ACTIVE'
                  AND (:warehouseId IS NULL OR w.id = :warehouseId)
                GROUP BY w.id, w.name
                ORDER BY w.name ASC
                LIMIT :limit
                """
        );

        query.setParameter("warehouseId", warehouseId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> {
                    long occupiedLocations = getLong(row[2]);
                    long totalLocations = getLong(row[3]);
                    double utilizationPercent = totalLocations == 0
                            ? 0
                            : (occupiedLocations * 100.0) / totalLocations;

                    return DashboardWarehouseCapacityResponse.builder()
                            .warehouseId((String) row[0])
                            .warehouseName((String) row[1])
                            .occupiedLocations(occupiedLocations)
                            .totalLocations(totalLocations)
                            .utilizationPercent(utilizationPercent)
                            .alertLevel(resolveAlertLevel(utilizationPercent))
                            .build();
                })
                .toList();
    }

    @Override
    public List<DashboardActivityResponse> getRecentActivities(String warehouseId, int limit) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    sm.id AS movementId,
                    sm.movement_type AS movementType,
                    w.name AS warehouseName,
                    COALESCE(l.name, 'Chưa gán vị trí') AS locationName,
                    p.sku AS productSku,
                    p.name AS productName,
                    sm.reference_number AS referenceNumber,
                    sm.quantity_change AS quantityChange,
                    sm.movement_date AS movementDate
                FROM stock_movements sm
                JOIN products p ON p.id = sm.product_id
                JOIN warehouses w ON w.id = sm.warehouse_id
                LEFT JOIN locations l ON l.id = sm.location_id
                WHERE (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                ORDER BY sm.movement_date DESC
                LIMIT :limit
                """
        );

        query.setParameter("warehouseId", warehouseId);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> DashboardActivityResponse.builder()
                        .movementId((String) row[0])
                        .movementType(row[1] != null ? row[1].toString() : null)
                        .warehouseName((String) row[2])
                        .locationName((String) row[3])
                        .productSku((String) row[4])
                        .productName((String) row[5])
                        .referenceNumber((String) row[6])
                        .quantityChange(getBigDecimal(row[7]))
                        .movementDate(getLocalDateTime(row[8]))
                        .build())
                .toList();
    }

    private String resolveAlertLevel(double utilizationPercent) {
        if (utilizationPercent >= CAPACITY_WARNING_PERCENT) {
            return "HIGH";
        }
        if (utilizationPercent >= 50) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private BigDecimal getBigDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
    }

    private Integer getInteger(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Long getLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private LocalDateTime getLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported datetime value type: " + value.getClass().getName());
    }
}



