package org.demo.whs.entity;

import jakarta.persistence.Column;
import lombok.*;
import org.demo.whs.entity.enums.RoleType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role extends BaseEntity {
    @Column(nullable = false)
    private RoleType role;

    private String description;
}
