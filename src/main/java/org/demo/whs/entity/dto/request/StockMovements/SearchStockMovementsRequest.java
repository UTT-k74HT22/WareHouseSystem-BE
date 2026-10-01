package org.demo.whs.entity.dto.request.StockMovements;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;

import java.time.LocalDate;

/**
 * Search filter for stock movements (traceability history).
 * All fields are optional; null means no filtering on that dimension.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchStockMovementsRequest {

    private String productId;

    private String warehouseId;

    private String locationId;

    private String batchId;

    private StockMovementsType movementType;

    private ReferenceType referenceType;

    /** Inclusive start day (bound from query param yyyy-MM-dd). */
    private LocalDate movementDateFrom;

    /** Inclusive end day (bound from query param yyyy-MM-dd). */
    private LocalDate movementDateTo;

}
