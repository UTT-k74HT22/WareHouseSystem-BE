package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uk_inventory_location", columnNames = {"product_id", "warehouse_id", "location_id", "batch_id"})
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Products product;

    @Column(name = "product_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, insertable = false, updatable = false)
    private Warehouses warehouse;

    @Column(name = "warehouse_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String warehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    private Locations location;

    @Column(name = "location_id", length = 36, columnDefinition = "char(36)")
    private String locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", insertable = false, updatable = false)
    private Batch batch;

    @Column(name = "batch_id", length = 36, columnDefinition = "char(36)")
    private String batchId;

    @Column(name = "on_hand_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;

    public BigDecimal getAvailableQuantity() {
        if (onHandQuantity == null) return BigDecimal.ZERO;
        if (reservedQuantity == null) return onHandQuantity;
        return onHandQuantity.subtract(reservedQuantity);
    }
}
