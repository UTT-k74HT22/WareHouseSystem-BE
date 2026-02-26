package org.demo.whs.mapper;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Mapper class for converting between Product entities and DTOs.
 */
@Component
public class ProductMapper {

    /**
     * Converts a CreateProductRequest DTO to a Products entity.
     *
     * @param request the CreateProductRequest DTO
     * @return the corresponding Products entity
     */
    public Products toEntity(CreateProductRequest request) {
        return Products.builder()
                .sku(request.getSku().toUpperCase()) // Normalize SKU to uppercase
                .name(request.getName())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .uomId(request.getUomId())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .status(ProductStatus.ACTIVE) // Default status
                .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : BigDecimal.ZERO)
                .maxStockLevel(request.getMaxStockLevel())
                .reOrderPoint(request.getReorderPoint())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .barcode(request.getBarcode())
                .imageUrl(request.getImageUrl())
                .requiresBatchTracking(request.getRequiresBatchTracking() != null ? request.getRequiresBatchTracking() : false)
                .build();
    }

    /**
     * Converts a Products entity to a ProductResponse DTO.
     *
     * @param product the Products entity
     * @return the corresponding ProductResponse DTO
     */
    public ProductResponse toResponse(Products product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categoryName(null) // Will be populated by service if needed
                .uomId(product.getUomId())
                .uomCode(null) // Will be populated by service if needed
                .uomName(null) // Will be populated by service if needed
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .status(product.getStatus())
                .minStockLevel(product.getMinStockLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .reorderPoint(product.getReOrderPoint())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .barcode(product.getBarcode())
                .imageUrl(product.getImageUrl())
                .requiresBatchTracking(product.getRequiresBatchTracking())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .updatedBy(product.getUpdatedBy())
                .build();
    }

    /**
     * Converts a Products entity to a ProductResponse DTO with related data.
     *
     * @param product  the Products entity
     * @param category the category entity (can be null)
     * @param uom      the unit of measure entity (can be null)
     * @return the corresponding ProductResponse DTO
     */
    public ProductResponse toResponse(Products product, Category category, UnitsOfMeasure uom) {
        ProductResponse response = toResponse(product);

        if (category != null || uom != null) {
            response = ProductResponse.builder()
                    .id(response.getId())
                    .sku(response.getSku())
                    .name(response.getName())
                    .description(response.getDescription())
                    .categoryId(response.getCategoryId())
                    .categoryName(category != null ? category.getName() : null)
                    .uomId(response.getUomId())
                    .uomCode(uom != null ? uom.getCode() : null)
                    .uomName(uom != null ? uom.getName() : null)
                    .weight(response.getWeight())
                    .dimensions(response.getDimensions())
                    .status(response.getStatus())
                    .minStockLevel(response.getMinStockLevel())
                    .maxStockLevel(response.getMaxStockLevel())
                    .reorderPoint(response.getReorderPoint())
                    .costPrice(response.getCostPrice())
                    .sellingPrice(response.getSellingPrice())
                    .barcode(response.getBarcode())
                    .imageUrl(response.getImageUrl())
                    .requiresBatchTracking(response.getRequiresBatchTracking())
                    .createdAt(response.getCreatedAt())
                    .updatedAt(response.getUpdatedAt())
                    .createdBy(response.getCreatedBy())
                    .updatedBy(response.getUpdatedBy())
                    .build();
        }

        return response;
    }

    /**
     * Converts a list of Products entities to a list of ProductResponse DTOs.
     *
     * @param products the list of Products entities
     * @return the corresponding list of ProductResponse DTOs
     */
    public List<ProductResponse> toResponses(List<Products> products) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Converts a list of Products entities to a list of ProductResponse DTOs with related data.
     *
     * @param products     the list of Products entities
     * @param categoryMap  map of category ID to Category entity
     * @param uomMap       map of UOM ID to UnitsOfMeasure entity
     * @return the corresponding list of ProductResponse DTOs
     */
    public List<ProductResponse> toResponses(List<Products> products,
                                             Map<String, Category> categoryMap,
                                             Map<String, UnitsOfMeasure> uomMap) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(product -> toResponse(
                        product,
                        categoryMap.get(product.getCategoryId()),
                        uomMap.get(product.getUomId())
                ))
                .toList();
    }

    /**
     * Updates an existing Products entity with data from UpdateProductRequest.
     * Only updates fields that are not null in the request.
     *
     * @param entity  the existing Products entity to update
     * @param request the UpdateProductRequest DTO containing new data
     */
    public void updateEntity(Products entity, UpdateProductRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            entity.setCategoryId(request.getCategoryId());
        }
        if (request.getUomId() != null && !request.getUomId().isBlank()) {
            entity.setUomId(request.getUomId());
        }
        if (request.getWeight() != null) {
            entity.setWeight(request.getWeight());
        }
        if (request.getDimensions() != null) {
            entity.setDimensions(request.getDimensions());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getMinStockLevel() != null) {
            entity.setMinStockLevel(request.getMinStockLevel());
        }
        if (request.getMaxStockLevel() != null) {
            entity.setMaxStockLevel(request.getMaxStockLevel());
        }
        if (request.getReorderPoint() != null) {
            entity.setReOrderPoint(request.getReorderPoint());
        }
        if (request.getCostPrice() != null) {
            entity.setCostPrice(request.getCostPrice());
        }
        if (request.getSellingPrice() != null) {
            entity.setSellingPrice(request.getSellingPrice());
        }
        if (request.getBarcode() != null) {
            entity.setBarcode(request.getBarcode());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl());
        }
        if (request.getRequiresBatchTracking() != null) {
            entity.setRequiresBatchTracking(request.getRequiresBatchTracking());
        }
    }
}
