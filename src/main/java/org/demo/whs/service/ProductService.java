package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;

import java.util.List;

/**
 * Service interface for product-related operations.
 */
public interface ProductService {

    /**
     * Create a new product.
     *
     * @param request the product creation request
     * @return the created product response
     */
    ProductResponse createProduct(CreateProductRequest request);

    /**
     * Update an existing product.
     *
     * @param id      the product ID
     * @param request the product update request
     * @return the updated product response
     */
    ProductResponse updateProduct(String id, UpdateProductRequest request);

    /**
     * Get a product by ID.
     *
     * @param id the product ID
     * @return the product response
     */
    ProductResponse getProductById(String id);

    /**
     * Get a product by SKU (case-insensitive).
     *
     * @param sku the product SKU
     * @return the product response
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Get all products with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated product responses
     */
    PageResponse<ProductResponse> getAllProducts(Integer page, Integer size);

    /**
     * Search products with filters and pagination.
     *
     * @param request  the search request with filters
     * @param page     the page number (0-based)
     * @param size     the page size
     * @return paginated product responses
     */
    PageResponse<ProductResponse> searchProducts(SearchProductRequest request, Integer page, Integer size);

    /**
     * Delete a product by ID (soft delete by changing status to DISCONTINUED).
     *
     * @param id the product ID
     */
    void deleteProduct(String id);

    /**
     * Get products by category ID.
     *
     * @param categoryId the category ID
     * @param page       the page number (0-based)
     * @param size       the page size
     * @return paginated product responses
     */
    PageResponse<ProductResponse> getProductsByCategory(String categoryId, Integer page, Integer size);

    /**
     * Get products that require batch tracking.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated product responses
     */
    PageResponse<ProductResponse> getBatchTrackingProducts(Integer page, Integer size);
}


