package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.ReasonType;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments")
@Check(constraints = "quantity_before >= 0 AND quantity_after >= 0 " +
        "AND adjustment_quantity = quantity_after - quantity_before " +
        "AND adjustment_quantity <> 0 " +
        "AND ((status <> 'APPROVED') OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)) " +
        "AND ((status <> 'REJECTED') OR (rejection_reason IS NOT NULL))")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustments extends BaseEntity {

    @Column(name = "adjustment_number", nullable = false, length = 50, unique = true)
    private String adjustmentNumber;

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

    @Column(name = "quantity_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityAfter;

    @Column(name = "adjustment_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal adjustmentQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private ReasonType reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockAdjustmentsStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval;

    @Column(name = "approved_by", columnDefinition = "char(36)")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
