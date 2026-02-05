package org.demo.whs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity class representing Sales Order Lines.
 */
@Entity
@Table(name = "sales_order_lines")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderLines extends BaseEntity {

    @Column(name = "sales_order_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String salesOrderId;

    @Column(name = "product_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String productId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity_ordered", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityOrdered;

    @Column(name = "quantity_shipped", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityShipped;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes", columnDefinition = ("TEXT"))
    private String notes;
}
