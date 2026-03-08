package org.demo.whs.entity.dto.request.BusinessPartner;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchBusinessPartnerRequest {

    private String code;

    private String name;

    private BusinessPartnerType type;

    private BusinessPartnerStatus status;

}
