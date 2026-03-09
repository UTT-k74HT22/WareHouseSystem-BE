package org.demo.whs.entity.dto.request.InboundReceipts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateInboundReceiptsRequest {

    private LocalDate receiptDate;

    @Size(max = 100, message = "Delivery note number must not exceed 100 characters")
    private String deliveryNoteNumber;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
