package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        indexes = {
                @Index(name = "idx_inventory_product", columnList = "product_id"),
                @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_inventory_location", columnList = "location_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_location",
                        columnNames = {"product_id","warehouse_id","location_id","batch_id"}
                )
        }
)
@SqlResultSetMappings({

        @SqlResultSetMapping(
                name = "InventorySummaryResponseMapping",
                classes = @ConstructorResult(
                        targetClass = InventorySummaryResponse.class,
                        columns = {
                                @ColumnResult(name = "productId", type = String.class),
                                @ColumnResult(name = "productSku", type = String.class),
                                @ColumnResult(name = "productName", type = String.class),
                                @ColumnResult(name = "totalOnHandQuantity", type = BigDecimal.class),
                                @ColumnResult(name = "totalReservedQuantity", type = BigDecimal.class),
                                @ColumnResult(name = "warehouseCount", type = Long.class),
                                @ColumnResult(name = "locationCount", type = Long.class)
                        }
                )
        ),

        @SqlResultSetMapping(
                name = "InventoryByLocationResponseMapping",
                classes = @ConstructorResult(
                        targetClass = InventoryByLocationResponse.class,
                        columns = {
                                @ColumnResult(name = "locationId", type = String.class),
                                @ColumnResult(name = "locationName", type = String.class),
                                @ColumnResult(name = "productId", type = String.class),
                                @ColumnResult(name = "productName", type = String.class),
                                @ColumnResult(name = "totalQuantity", type = BigDecimal.class)
                        }
                )
        ),

        @SqlResultSetMapping(
                name = "InventoryAvailabilityResponseMapping",
                classes = @ConstructorResult(
                        targetClass = CheckAvailabilityResponse.class,
                        columns = {
                                @ColumnResult(name = "totalOnHandQuantity", type = BigDecimal.class),
                                @ColumnResult(name = "totalReservedQuantity", type = BigDecimal.class)
                        }
                )
        )
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @Column(name = "product_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "location_id", length = 36, columnDefinition = "char(36)")
    private String locationId;

    @Column(name = "batch_id", length = 36, columnDefinition = "char(36)")
    private String batchId;

    @Builder.Default
    @Column(name = "on_hand_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;

    public BigDecimal getAvailableQuantity() {
        return onHandQuantity.subtract(reservedQuantity);
    }
}