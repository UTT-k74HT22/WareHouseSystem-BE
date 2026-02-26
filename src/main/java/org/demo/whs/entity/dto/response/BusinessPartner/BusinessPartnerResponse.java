package org.demo.whs.entity.dto.response.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

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
    private String type;

    private String contactPerson;
    private String email;
    private String phone;

    private String address;
    private String city;
    private String country;

    private String taxId;
    private String paymentTerms;
    private BigDecimal creditLimit;

    private String status;
    private String notes;

    private Integer purchaseOrderCount;
    private Integer salesOrderCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
