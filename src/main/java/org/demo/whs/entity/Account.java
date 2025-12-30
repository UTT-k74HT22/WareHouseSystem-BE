package org.demo.whs.entity;

import jakarta.persistence.Column;
import lombok.*;
import org.demo.whs.entity.enums.AccountStatus;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private AccountStatus status;
}
