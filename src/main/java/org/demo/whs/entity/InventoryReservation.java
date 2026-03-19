package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.demo.whs.entity.enums.InventoryReservationStatus;

import java.math.BigDecimal;

/**
 * Entity to track inventory reservations for products and order lines.
 * This provides idempotency and allows for releasing/consuming reservations later.
 */
@Entity
@Table(name = "inventory_reservations", indexes = {
        @Index(name = "idx_reservation_request_key", columnList = "request_key"),
        @Index(name = "idx_reservation_order_line", columnList = "order_line_id"),
        @Index(name = "idx_reservation_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservation extends BaseEntity {

    @Column(name = "inventory_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String inventoryId;

    @Column(name = "product_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "location_id", length = 36, columnDefinition = "char(36)")
    private String locationId;

    @Column(name = "batch_id", length = 36, columnDefinition = "char(36)")
    private String batchId;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "order_line_id", length = 100)
    private String orderLineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InventoryReservationStatus status = InventoryReservationStatus.RESERVED;
}
