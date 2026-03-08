package org.demo.whs.entity.dto.request.BusinessPartner;

import lombok.Data;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;

@Data
public class SearchBusinessPartnerRequest {

    private String code;

    private String name;

    private BusinessPartnerType type;

    private BusinessPartnerStatus status;

}
