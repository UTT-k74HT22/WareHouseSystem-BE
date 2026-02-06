package org.demo.whs.entity.dto.response.Product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for product information.
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductResponse {

    private String id;
    private String sku;
    private String name;
    private String description;
    private String categoryId;
    private String categoryName;
    private String uomId;
    private String uomCode;
    private String uomName;
    private BigDecimal weight;
    private String dimensions;
    private ProductStatus status;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private BigDecimal reorderPoint;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String barcode;
    private String imageUrl;
    private Boolean requiresBatchTracking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
