package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleHasPermission {

    @Id
    @Column(name = "role_id")
    private String roleId;

    @Id
    @Column(name = "permission_id")
    private String permissionId;
}