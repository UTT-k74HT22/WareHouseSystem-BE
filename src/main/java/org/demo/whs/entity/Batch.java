package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.BatchStatus;
import java.time.LocalDate;

@Entity
@Table(name = "batches")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Batch extends BaseEntity {

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "product_id", nullable = false, columnDefinition = "char(36)")
    private String productId;

    @Column(name = "manufacturing_date", nullable = false)
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "supplier_batch_number", length = 50)
    private String supplierBatchNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;
}
