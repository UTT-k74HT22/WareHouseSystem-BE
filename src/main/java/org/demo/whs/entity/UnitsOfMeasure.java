package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.UnitsOfMeasureType;

@Entity
@Table(name = "units_of_measure")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnitsOfMeasure extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private UnitsOfMeasureType type;
}
