package org.demo.whs.entity;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleHasPermission extends BaseEntity {
    private String roleId;
    private String permissionId;
}
