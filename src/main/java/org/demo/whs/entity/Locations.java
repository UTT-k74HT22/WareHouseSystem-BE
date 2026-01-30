package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;

import java.math.BigDecimal;

@Entity
@Table(name = "locations")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Locations extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "char(36)")
    private String warehouseId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "zone", length = 50)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LocationType type;

    @Column(name = "capacity")
    private BigDecimal capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LocationStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
