package org.demo.whs.entity.dto.request.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessPartnerRequest {
    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    /**
     * customer | supplier | both
     */
    @NotBlank(message = "Type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Size(max = 255, message = "Contact person must not exceed 255 characters")
    private String contactPerson;

    @Email(message = "Email invalid format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "Phone invalid format")
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    @DecimalMin(value = "0.0", inclusive = true, message = "Credit limit must be greater than or equal to 0")
    private BigDecimal creditLimit;

    /**
     * active | inactive
     */
    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;


    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
