package org.demo.whs.entity.dto.request.BusinessPartner;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UpdateBusinessPartnerRequest {
    @Size(max = 255)
    private String name;

    private String type;

    private String contactPerson;

    @Email
    private String email;

    private String phone;

    private String address;

    private String city;

    private String country;

    private String taxId;

    private String paymentTerms;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal creditLimit;

    private String status;

    private String notes;
}
