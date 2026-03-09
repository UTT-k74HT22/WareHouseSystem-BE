package org.demo.whs.entity.dto.response.InboundReceipts;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InboundReceiptsResponse {

    private String id;
    private String receiptNumber;
    private String purchaseOrderId;
    private String purchaseOrderNumber;
    private String warehouseId;
    private String warehouseName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiptDate;

    private String status;
    private String deliveryNoteNumber;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;

    private String confirmedBy;
    private List<InboundReceiptLinesResponse> lines;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
