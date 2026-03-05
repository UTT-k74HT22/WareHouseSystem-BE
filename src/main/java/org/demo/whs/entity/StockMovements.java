package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing stock movements in the warehouse system.
 */
@Entity
@Table(name = "stock_movements")
@Check(constraints = "quantity_after = quantity_before + quantity_change AND quantity_before >= 0 AND quantity_after >= 0")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMovements extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private StockMovementsType movementType;

    @Column(name = "product_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String productId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String warehouseId;

    @Column(name = "location_id", columnDefinition = ("CHAR(36)"))
    private String locationId;

    @Column(name = "batch_id", columnDefinition = ("CHAR(36)"))
    private String batchId;

    @Column(name = "quantity_change", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityChange;

    @Column(name = "quantity_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityAfter;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Column(name = "reference_id", columnDefinition = ("CHAR(36)"))
    private String referenceId;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "notes", columnDefinition = ("TEXT"))
    private String notes;
}
