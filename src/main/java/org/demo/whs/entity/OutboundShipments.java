package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing outbound shipments in the warehouse management system.
 */
@Entity
@Table(name = "outbound_shipments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboundShipments extends BaseEntity {

    @Column(name = "shipment_number", nullable = false, unique = true)
    private String shipmentNumber;

    @Column(name = "sales_order_id", nullable = false, columnDefinition = "CHAR(36)")
    private String salesOrderId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "CHAR(36)")
    private String warehouseId;

    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboundShipmentsStatus status;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "confirmed_by", columnDefinition = ("CHAR(36)"))
    private String confirmedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
