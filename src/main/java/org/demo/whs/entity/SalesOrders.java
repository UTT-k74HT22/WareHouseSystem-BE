package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.CurrencyType;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a sales order in the warehouse management system.
 */
@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrders extends BaseEntity {

    @Column(name = "so_number", nullable = false, unique = true)
    private String soNumber;

    @Column(name = "customer_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String customerId;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = ("CHAR(36)"))
    private String warehouseId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "requested_delivery_date", nullable = false)
    private LocalDate requestedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrdersStatus status;

    @Column(name = "sub_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyType currency;

    @Column(name = "notes", columnDefinition = ("TEXT"))
    private String notes;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by", columnDefinition = ("CHAR(36)"))
    private String confirmedBy;
}
