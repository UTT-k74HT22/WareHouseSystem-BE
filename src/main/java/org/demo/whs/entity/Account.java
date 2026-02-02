package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.AccountStatus;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountHasRole> accountRoles = new ArrayList<>();

    public boolean isEmailVerified() {
        return emailVerified;
    }
}

