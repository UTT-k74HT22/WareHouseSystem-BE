package org.demo.whs.repository.projection;

import java.math.BigDecimal;

/**
 * Projection for inventory availability.
 */
public interface InventoryAvailabilityProjection {
    BigDecimal getTotalOnHandQuantity();
    BigDecimal getTotalReservedQuantity();
}
