package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import java.time.LocalDate;

@SqlResultSetMapping(
        name = "AccountResponseMapping",
        classes = @ConstructorResult(
                targetClass = AccountResponse.class,
                columns = {
                        @ColumnResult(name = "accountId", type = String.class),
                        @ColumnResult(name = "username", type = String.class),
                        @ColumnResult(name = "status", type = String.class),
                        @ColumnResult(name = "email", type = String.class),
                        @ColumnResult(name = "firstName", type = String.class),
                        @ColumnResult(name = "lastName", type = String.class)
                }
        )
)
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true, columnDefinition = ("CHAR(36)"))
    private String accountId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate date_of_birth;
}
