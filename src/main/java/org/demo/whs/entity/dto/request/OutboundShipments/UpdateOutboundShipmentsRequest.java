package org.demo.whs.entity.dto.request.OutboundShipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOutboundShipmentsRequest {
    private LocalDate shipmentDate;
    private String trackingNumber;
    private String carrier;
    private String notes;
}
