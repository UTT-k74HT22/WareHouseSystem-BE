package org.demo.whs.entity.dto.request.OutboundShipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundShipmentsFilterRequest {
    private String shipmentNumber;
    private String salesOrderId;
    private String warehouseId;
    private OutboundShipmentsStatus status;
    private LocalDate shipmentDateFrom;
    private LocalDate shipmentDateTo;
}
