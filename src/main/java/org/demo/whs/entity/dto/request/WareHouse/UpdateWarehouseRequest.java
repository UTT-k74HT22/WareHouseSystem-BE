package org.demo.whs.entity.dto.request.WareHouse;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import org.demo.whs.entity.enums.WareHouseType;

import java.math.BigDecimal;

/**
 * Request DTO for updating warehouse information.
 * All fields are optional - user can update or leave blank.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateWarehouseRequest {

    private String name;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    @Email(message = "Email must be valid format")
    private String email;

    private WareHouseType wareHouseType;

    private BigDecimal capacity;

    private String managerId;
}
