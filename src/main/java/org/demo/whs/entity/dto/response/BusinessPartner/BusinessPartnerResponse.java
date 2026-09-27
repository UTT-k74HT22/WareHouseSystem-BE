package org.demo.whs.entity.dto.response.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessPartnerResponse {

    private String id;

    private String code;
    private String name;
    private BusinessPartnerType type;

    private String contactPerson;
    private String email;
    private String phone;

    private String address;
    private String city;
    private String country;

    private String taxId;
    private String paymentTerms;
    private BigDecimal creditLimit;

    private BusinessPartnerStatus status;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
