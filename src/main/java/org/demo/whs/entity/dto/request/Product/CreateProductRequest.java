package org.demo.whs.entity.dto.request.Product;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new product.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "SKU must contain only uppercase letters, numbers, hyphens, and underscores")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Unit of Measure ID is required")
    private String uomId;

    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    @Digits(integer = 10, fraction = 3, message = "Weight must have at most 10 integer digits and 3 decimal places")
    private BigDecimal weight;

    @Pattern(regexp = "^\\d+(\\.\\d+)?x\\d+(\\.\\d+)?x\\d+(\\.\\d+)?$",
             message = "Dimensions must be in format LxWxH (e.g., 10x20x30 or 10.5x20.5x30.5)")
    @Size(max = 50, message = "Dimensions must not exceed 50 characters")
    private String dimensions;

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
    @Pattern(regexp = "^[a-zA-Z0-9/_\\-.]*$", message = "Invalid image path format")
    private String imageUrl;

    private Boolean requiresBatchTracking;
}
