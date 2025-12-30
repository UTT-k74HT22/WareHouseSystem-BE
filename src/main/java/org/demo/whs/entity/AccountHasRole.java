package org.demo.whs.entity;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountHasRole extends BaseEntity {
    private String roleId;
    private String accountId;
}
