package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.InboundReceiptsStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing inbound receipts in the warehouse system.
 */
@Entity
@Table(name = "inbound_receipts")
@Getter
@Setter

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InboundReceipts extends BaseEntity {

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @Column(name = "purchase_order_id", nullable = false, columnDefinition = "char(36)")
    private String purchaseOrderId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InboundReceiptsStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 36, columnDefinition = ("char(36)"))
    private String confirmedBy;

    @Column(name = "delivery_note_number", length = 50)
    private String deliveryNoteNumber;
}
