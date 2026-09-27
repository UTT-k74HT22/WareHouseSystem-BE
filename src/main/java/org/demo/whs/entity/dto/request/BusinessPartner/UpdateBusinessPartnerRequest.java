package org.demo.whs.entity.dto.request.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;

import java.math.BigDecimal;

/**
 * Request DTO for updating a business partner.
 * All fields are optional.
 */
@Builder
@AllArgsConstructor
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateBusinessPartnerRequest {
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private BusinessPartnerType type;

    @Size(max = 100, message = "Contact person must not exceed 100 characters")
    private String contactPerson;

    @Email(message = "Email invalid format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Phone must be 10 digits starting with 0 or +84 followed by 9 digits")
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    @DecimalMin(value = "0.0", inclusive = true, message = "Credit limit must be greater than or equal to 0")
    private BigDecimal creditLimit;

    private BusinessPartnerStatus status;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
