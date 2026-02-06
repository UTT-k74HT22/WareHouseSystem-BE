package org.demo.whs.entity.dto.request.Product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.ProductStatus;

/**
 * Request DTO for searching and filtering products.
 * All fields are optional.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchProductRequest {

    /**
     * Search by SKU (exact match, case-insensitive)
     */
    private String sku;

    /**
     * Search by product name (partial match, case-insensitive)
     */
    private String name;

    /**
     * Filter by category ID
     */
    private String categoryId;

    /**
     * Filter by UOM ID
     */
    private String uomId;

    /**
     * Filter by product status
     */
    private ProductStatus status;

    /**
     * Filter by batch tracking requirement
     */
    private Boolean requiresBatchTracking;

    /**
     * Full-text search across name and description
     */
    private String searchText;
}
