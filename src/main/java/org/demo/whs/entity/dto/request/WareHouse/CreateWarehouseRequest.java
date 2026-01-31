package org.demo.whs.entity.dto.request.WareHouse;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.entity.enums.WareHouseType;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateWarehouseRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String address;

    @Pattern(regexp = "^\\d{10}$", message = "Phone invalid format")
    private String phone;

    @Email(message = "Email invalid format")
    private String email;

    @NotNull(message = "Status is required")
    private WareHouseStatus status;

    @NotNull(message = "Type is required")
    private WareHouseType wareHouseType;

    @NotBlank(message = "Manager ID is required")
    private String managerId;
}
