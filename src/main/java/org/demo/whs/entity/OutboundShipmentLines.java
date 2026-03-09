package org.demo.whs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing outbound shipment lines in the warehouse management system.
 */
@Entity
@Table(name = "outbound_shipment_lines")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboundShipmentLines extends BaseEntity {

    @Column(name = "outbound_shipment_id", nullable = false, columnDefinition = "CHAR(36)")
    private String outboundShipmentId;

    @Column(name = "sales_order_line_id", nullable = false, columnDefinition = "CHAR(36)")
    private String salesOrderLineId;

    @Column(name = "product_id", nullable = false, columnDefinition = "CHAR(36)")
    private String productId;

    @Column(name = "batch_id", columnDefinition = "CHAR(36)")
    private String batchId;

    @Column(name = "location_id", nullable = false, columnDefinition = "CHAR(36)")
    private String locationId;

    @Column(name = "quantity_shipped", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityShipped;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "picked_by", columnDefinition = "CHAR(36)")
    private String pickedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
