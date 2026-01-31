package org.demo.whs.entity.dto.response.WareHouse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WareHouseResponse {
    private String id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String status;
    private String wareHouseType;
    private String managerId;
}
