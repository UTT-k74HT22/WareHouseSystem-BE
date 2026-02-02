package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHasRole {

    @EmbeddedId
    private AccountRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    private Role role;
}
