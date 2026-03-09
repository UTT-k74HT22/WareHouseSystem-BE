package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.QualityStatus;

import java.math.BigDecimal;

/**
 * Entity class representing inbound receipt lines in the warehouse management system.
 */
@Entity
@Table(name = "inbound_receipt_lines")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InboundReceiptLines extends BaseEntity {

    @Column(name = "inbound_receipt_id", nullable = false, columnDefinition = "char(36)")
    private String inboundReceiptId;

    @Column(name = "purchase_order_line_id", nullable = false, columnDefinition = "char(36)")
    private String purchaseOrderLineId;

    @Column(name = "product_id", nullable = false, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "batch_id", columnDefinition = "char(36)")
    private String batchId;

    @Column(name = "location_id", nullable = false, columnDefinition = "char(36)")
    private String locationId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity_received", nullable = false)
    private BigDecimal quantityReceived;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false)
    private QualityStatus qualityStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
