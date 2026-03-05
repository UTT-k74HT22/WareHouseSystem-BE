package org.demo.whs.repository.projection;

import java.math.BigDecimal;

public interface InventorySummaryProjection {
    String getProductId();

    String getProductSku();

    String getProductName();

    BigDecimal getTotalOnHandQuantity();

    BigDecimal getTotalReservedQuantity();

    Long getWarehouseCount();

    Long getLocationCount();
}
