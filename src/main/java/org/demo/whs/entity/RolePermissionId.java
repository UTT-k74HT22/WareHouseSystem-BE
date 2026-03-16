package org.demo.whs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionId implements Serializable {

    @Column(name = "role_id", length = 36, columnDefinition = "CHAR(36)")
    private String roleId;

    @Column(name = "permission_id", length = 36, columnDefinition = "CHAR(36)")
    private String permissionId;

}
