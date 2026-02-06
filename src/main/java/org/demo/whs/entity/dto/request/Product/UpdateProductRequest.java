package org.demo.whs.entity.dto.request.Product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.ProductStatus;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing product.
 * All fields are optional except validation constraints.
 * SKU cannot be changed.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateProductRequest {

    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private String categoryId;

    private String uomId;

    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    @Digits(integer = 10, fraction = 3, message = "Weight must have at most 10 integer digits and 3 decimal places")
    private BigDecimal weight;

    @Pattern(regexp = "^\\d+(\\.\\d+)?x\\d+(\\.\\d+)?x\\d+(\\.\\d+)?$",
             message = "Dimensions must be in format LxWxH (e.g., 10x20x30 or 10.5x20.5x30.5)")
    @Size(max = 50, message = "Dimensions must not exceed 50 characters")
    private String dimensions;

    private ProductStatus status;

    @DecimalMin(value = "0", message = "Minimum stock level must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 2, message = "Min stock level must have at most 15 integer digits and 2 decimal places")
    private BigDecimal minStockLevel;

    @DecimalMin(value = "0", message = "Maximum stock level must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 2, message = "Max stock level must have at most 15 integer digits and 2 decimal places")
    private BigDecimal maxStockLevel;

    @DecimalMin(value = "0", message = "Reorder point must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 2, message = "Reorder point must have at most 15 integer digits and 2 decimal places")
    private BigDecimal reorderPoint;

    @DecimalMin(value = "0", message = "Cost price must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 2, message = "Cost price must have at most 15 integer digits and 2 decimal places")
    private BigDecimal costPrice;

    @DecimalMin(value = "0", message = "Selling price must be greater than or equal to 0")
    @Digits(integer = 15, fraction = 2, message = "Selling price must have at most 15 integer digits and 2 decimal places")
    private BigDecimal sellingPrice;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    private String barcode;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Pattern(regexp = "^(https?://.*|)$", message = "Image URL must be a valid HTTP/HTTPS URL or empty")
    private String imageUrl;

    private Boolean requiresBatchTracking;
}
