package org.demo.whs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @Column(name = "product_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "location_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String locationId;

    @Column(name = "batch_id", length = 36, columnDefinition = "char(36)")
    private String batchId;

    @Column(name = "on_hand_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal onHandQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal reservedQuantity;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;
}
