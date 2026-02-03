package org.demo.whs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity representing the lines of a purchase order.
 */
@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderLines extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, columnDefinition = "char(36)")
    private String purchaseOrderId;

    @Column(name = "product_id", nullable = false, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity_ordered", nullable = false)
    private BigDecimal quantityOrdered;

    @Column(name = "quantity_received", nullable = false)
    private BigDecimal quantityReceived;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
