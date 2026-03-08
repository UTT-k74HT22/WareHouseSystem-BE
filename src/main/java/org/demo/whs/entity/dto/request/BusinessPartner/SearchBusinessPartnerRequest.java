package org.demo.whs.entity.dto.request.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchBusinessPartnerRequest {

    private String code;

    private String name;

    private BusinessPartnerType type;

    private BusinessPartnerStatus status;

}
