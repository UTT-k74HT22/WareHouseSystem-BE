package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.CurrencyType;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a Purchase Order in the warehouse management system.
 */
@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrders extends BaseEntity {

    @Column(name = "purchase_order_number", nullable = false, unique = true, length = 50)
    private String purchaseOrderNumber;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "char(36)")
    private String supplierId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PurchaseOrdersStatus status;

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyType currency;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", length = 36, columnDefinition = ("char(36)"))
    private String confirmedBy;
}
