package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(
            name = "account_id",
            nullable = false,
            columnDefinition = "CHAR(36)"
    )
    private Account account;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate birthDate;
}
