package org.demo.whs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Product Management.
 * Provides endpoints for CRUD operations and search functionality.
 */
@RequestMapping("/api/v1/products")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product.
     *
     * POST /api/v1/products
     *
     * @param request the product creation request
     * @return ResponseEntity containing the created product
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_CREATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> createProduct(
            @RequestBody @Valid CreateProductRequest request) {
        log.info("Received request to create product with SKU={}", request.getSku());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Product created successfully"));
    }

    /**
     * Update an existing product.
     *
     * PUT /api/v1/products/{id}
     *
     * @param id      the product ID
     * @param request the product update request
     * @return ResponseEntity containing the updated product
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_UPDATE')")
    public ResponseEntity<BaseResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @RequestBody @Valid UpdateProductRequest request) {
        log.info("Received request to update product with ID={}", id);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Product updated successfully"));
    }

    /**
     * Get a product by ID.
     *
     * GET /api/v1/products/{id}
     *
     * @param id the product ID
     * @return ResponseEntity containing the product
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<ProductResponse>> getProductById(@PathVariable String id) {
        log.info("Received request to get product with ID={}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get a product by SKU (case-insensitive).
     *
     * GET /api/v1/products/sku/{sku}
     *
     * @param sku the product SKU
     * @return ResponseEntity containing the product
     */
    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        log.info("Received request to get product with SKU={}", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get all products with pagination.
     *
     * GET /api/v1/products?page=0&size=10
     *
     * @param page the page number (0-based, default: 0)
     * @param size the page size (default: 10)
     * @return ResponseEntity containing paginated products
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        log.info("Received request to get all products - page={}, size={}", page, size);
        PageResponse<ProductResponse> response = productService.getAllProducts(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Search products with filters.
     *
     * POST /api/v1/products/search?page=0&size=10
     *
     * @param request the search request with filters
     * @param page    the page number (0-based, default: 0)
     * @param size    the page size (default: 10)
     * @return ResponseEntity containing paginated search results
     */
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestBody SearchProductRequest request,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        log.info("Received request to search products - page={}, size={}", page, size);
        PageResponse<ProductResponse> response = productService.searchProducts(request, page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Delete a product (soft delete).
     *
     * DELETE /api/v1/products/{id}
     *
     * @param id the product ID
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_DELETE')")
    public ResponseEntity<BaseResponse<Void>> deleteProduct(@PathVariable String id) {
        log.info("Received request to delete product with ID={}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Product deleted successfully"));
    }

    /**
     * Get products by category.
     *
     * GET /api/v1/products/category/{categoryId}?page=0&size=10
     *
     * @param categoryId the category ID
     * @param page       the page number (0-based, default: 0)
     * @param size       the page size (default: 10)
     * @return ResponseEntity containing paginated products
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        log.info("Received request to get products by category ID={} - page={}, size={}", categoryId, page, size);
        PageResponse<ProductResponse> response = productService.getProductsByCategory(categoryId, page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get products that require batch tracking.
     *
     * GET /api/v1/products/batch-tracking?page=0&size=10
     *
     * @param page the page number (0-based, default: 0)
     * @param size the page size (default: 10)
     * @return ResponseEntity containing paginated products
     */
    @GetMapping("/batch-tracking")
    @PreAuthorize("hasAuthority('PERM_PRODUCT_READ')")
    public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getBatchTrackingProducts(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        log.info("Received request to get batch tracking products - page={}, size={}", page, size);
        PageResponse<ProductResponse> response = productService.getBatchTrackingProducts(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}


