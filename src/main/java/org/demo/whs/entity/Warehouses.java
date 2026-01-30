package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.entity.enums.WareHouseType;

import java.math.BigDecimal;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Warehouses extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WareHouseType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WareHouseStatus status;

    @Column(name = "capacity")
    private BigDecimal capacity;

    @Column(name = "manager_id", columnDefinition = "char(36)")
    private String managerId;
}
