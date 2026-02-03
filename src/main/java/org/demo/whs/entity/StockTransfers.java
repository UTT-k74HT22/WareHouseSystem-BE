package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.StockTransfersReason;
import org.demo.whs.entity.enums.StockTransfersStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockTransfers extends BaseEntity {

    @Column(name = "transfer_number", nullable = false, length = 50, unique = true)
    private String transferNumber;

    @Column(name = "product_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "from_location_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String fromLocationId;

    @Column(name = "to_location_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String toLocationId;

    @Column(name = "batch_id", length = 36, columnDefinition = "char(36)")
    private String batchId;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private StockTransfersReason reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockTransfersStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
