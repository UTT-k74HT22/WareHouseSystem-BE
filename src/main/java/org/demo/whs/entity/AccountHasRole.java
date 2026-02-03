package org.demo.whs.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "account_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountHasRole {

    @EmbeddedId
    private AccountRoleId id;
}
