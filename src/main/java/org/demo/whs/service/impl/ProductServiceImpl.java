package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Category;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.ProductMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ProductService interface.
 * Handles all product-related business logic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitsOfMeasureRepository unitsOfMeasureRepository;
    private final AccountRepository accountRepository;
    private final ProductMapper productMapper;

    /**
     * Create a new product with validation.
     *
     * @param request the product creation request
     * @return the created product response
     */
    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU={}", request.getSku());

        // 1. Validate SKU uniqueness (case-insensitive)
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            log.warn("Product SKU already exists: {}", request.getSku());
            throw new BadRequestException(ErrorCode.PROD_002);
        }

        // 2. Validate Category exists and is ACTIVE
        Category category = validateCategory(request.getCategoryId());

        // 3. Validate UOM exists
        UnitsOfMeasure uom = validateUom(request.getUomId());

        // 4. Validate stock level constraints
        validateStockLevels(request.getMinStockLevel(), request.getMaxStockLevel(), request.getReorderPoint());

        // 5. Map to entity
        Products product = productMapper.toEntity(request);

        // 6. Set audit fields
        Account currentUser = getCurrentUser();
        setAuditFields(product, currentUser, true);

        // 7. Save product
        Products savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID={}, SKU={}", savedProduct.getId(), savedProduct.getSku());

        // 8. Return response with related data
        return productMapper.toResponse(savedProduct, category, uom);
    }

    /**
     * Update an existing product.
     *
     * @param id      the product ID
     * @param request the product update request
     * @return the updated product response
     */
    @Override
    @Transactional
    public ProductResponse updateProduct(String id, UpdateProductRequest request) {
        log.info("Updating product with ID={}", id);

        // 1. Find existing product
        Products product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID={}", id);
                    return new NotFoundException("Product not found", ErrorCode.PROD_001);
                });

        // 2. If category is being updated, validate it
        Category category;
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            category = validateCategory(request.getCategoryId());
        } else {
            category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        }

        // 3. If UOM is being updated, validate it
        UnitsOfMeasure uom;
        if (request.getUomId() != null && !request.getUomId().isBlank()) {
            uom = validateUom(request.getUomId());
        } else {
            uom = unitsOfMeasureRepository.findById(product.getUomId()).orElse(null);
        }

        // 4. Validate batch tracking changes
        if (request.getRequiresBatchTracking() != null &&
            !request.getRequiresBatchTracking().equals(product.getRequiresBatchTracking())) {
            validateBatchTrackingChange(product, request.getRequiresBatchTracking());
        }

        // 5. Validate stock level constraints if any are being updated
        BigDecimal newMin = request.getMinStockLevel() != null ? request.getMinStockLevel() : product.getMinStockLevel();
        BigDecimal newMax = request.getMaxStockLevel() != null ? request.getMaxStockLevel() : product.getMaxStockLevel();
        BigDecimal newReorder = request.getReorderPoint() != null ? request.getReorderPoint() : product.getReOrderPoint();
        validateStockLevels(newMin, newMax, newReorder);

        // 6. Update entity
        productMapper.updateEntity(product, request);

        // 7. Update audit fields
        Account currentUser = getCurrentUser();
        setAuditFields(product, currentUser, false);

        // 8. Save changes
        Products updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID={}, SKU={}", updatedProduct.getId(), updatedProduct.getSku());

        // 9. Return response
        return productMapper.toResponse(updatedProduct, category, uom);
    }

    /**
     * Get a product by ID.
     *
     * @param id the product ID
     * @return the product response
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        log.info("Fetching product with ID={}", id);

        Products product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID={}", id);
                    return new NotFoundException("Product not found", ErrorCode.PROD_001);
                });

        // Fetch related data
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        UnitsOfMeasure uom = unitsOfMeasureRepository.findById(product.getUomId()).orElse(null);

        return productMapper.toResponse(product, category, uom);
    }

    /**
     * Get a product by SKU (case-insensitive).
     *
     * @param sku the product SKU
     * @return the product response
     */
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product with SKU={}", sku);

        Products product = productRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> {
                    log.warn("Product not found with SKU={}", sku);
                    return new NotFoundException("Product not found", ErrorCode.PROD_001);
                });

        // Fetch related data
        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        UnitsOfMeasure uom = unitsOfMeasureRepository.findById(product.getUomId()).orElse(null);

        return productMapper.toResponse(product, category, uom);
    }

    /**
     * Get all products with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated product responses
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Integer page, Integer size) {
        log.info("Fetching all products - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Products> productPage = productRepository.findAll(pageable);

        return buildPageResponse(productPage);
    }

    /**
     * Search products with filters and pagination.
     *
     * @param request  the search request with filters
     * @param page     the page number (0-based)
     * @param size     the page size
     * @return paginated product responses
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(SearchProductRequest request, Integer page, Integer size) {
        log.info("Searching products with filters - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Products> productPage = productRepository.searchProducts(
                request.getSku(),
                request.getName(),
                request.getCategoryId(),
                request.getUomId(),
                request.getStatus(),
                request.getRequiresBatchTracking(),
                pageable
        );

        return buildPageResponse(productPage);
    }

    /**
     * Delete a product by ID (soft delete).
     *
     * @param id the product ID
     */
    @Override
    @Transactional
    public void deleteProduct(String id) {
        log.info("Soft deleting product with ID={}", id);

        Products product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID={}", id);
                    return new NotFoundException("Product not found", ErrorCode.PROD_001);
                });

        // Soft delete by changing status
        product.setStatus(ProductStatus.DISCONTINUED);

        Account currentUser = getCurrentUser();
        product.setUpdatedBy(currentUser.getId());
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);
        log.info("Product soft deleted successfully with ID={}", id);
    }

    /**
     * Get products by category ID.
     *
     * @param categoryId the category ID
     * @param page       the page number (0-based)
     * @param size       the page size
     * @return paginated product responses
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(String categoryId, Integer page, Integer size) {
        log.info("Fetching products by category ID={} - page={}, size={}", categoryId, page, size);

        // Validate category exists
        validateCategory(categoryId);

        SearchProductRequest request = new SearchProductRequest();
        request.setCategoryId(categoryId);

        return searchProducts(request, page, size);
    }

    /**
     * Get products that require batch tracking.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated product responses
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getBatchTrackingProducts(Integer page, Integer size) {
        log.info("Fetching batch tracking products - page={}, size={}", page, size);

        SearchProductRequest request = new SearchProductRequest();
        request.setRequiresBatchTracking(true);

        return searchProducts(request, page, size);
    }

    // ========== Private Helper Methods ==========

    /**
     * Build page response with related data.
     */
    private PageResponse<ProductResponse> buildPageResponse(Page<Products> productPage) {
        List<Products> products = productPage.getContent();

        // Fetch related data in batch
        Map<String, Category> categoryMap = fetchCategoryMap(products);
        Map<String, UnitsOfMeasure> uomMap = fetchUomMap(products);

        // Map to responses
        List<ProductResponse> responses = productMapper.toResponses(products, categoryMap, uomMap);

        return PageResponse.from(productPage, responses);
    }

    /**
     * Validate category exists and is ACTIVE.
     */
    private Category validateCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found with ID={}", categoryId);
                    return new NotFoundException("Category not found", ErrorCode.PROD_004);
                });

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            log.warn("Category is not active with ID={}", categoryId);
            throw new BadRequestException(ErrorCode.PROD_004);
        }

        return category;
    }

    /**
     * Validate UOM exists.
     */
    private UnitsOfMeasure validateUom(String uomId) {
        return unitsOfMeasureRepository.findById(uomId)
                .orElseThrow(() -> {
                    log.warn("Unit of Measure not found with ID={}", uomId);
                    return new NotFoundException("Unit of Measure not found", ErrorCode.PROD_005);
                });
    }

    /**
     * Validate stock level constraints.
     */
    private void validateStockLevels(BigDecimal minStockLevel, BigDecimal maxStockLevel, BigDecimal reorderPoint) {
        // If max is set, it must be >= min
        if (maxStockLevel != null && minStockLevel != null && maxStockLevel.compareTo(minStockLevel) < 0) {
            log.warn("Max stock level ({}) is less than min stock level ({})", maxStockLevel, minStockLevel);
            throw new BadRequestException(ErrorCode.PROD_007);
        }

        // If reorder point is set, it must be between min and max
        if (reorderPoint != null && minStockLevel != null && reorderPoint.compareTo(minStockLevel) < 0) {
            log.warn("Reorder point ({}) is less than min stock level ({})", reorderPoint, minStockLevel);
            throw new BadRequestException(ErrorCode.PROD_008);
        }

        if (reorderPoint != null && maxStockLevel != null && reorderPoint.compareTo(maxStockLevel) > 0) {
            log.warn("Reorder point ({}) is greater than max stock level ({})", reorderPoint, maxStockLevel);
            throw new BadRequestException(ErrorCode.PROD_008);
        }
    }

    /**
     * Validate batch tracking changes.
     * TODO: In future, check if batch inventory exists before disabling batch tracking.
     */
    private void validateBatchTrackingChange(Products product, Boolean newValue) {
        if (product.getRequiresBatchTracking() && !newValue) {
            // Disabling batch tracking
            // TODO: Check if batch inventory exists
            // For now, we allow it
            log.warn("Disabling batch tracking for product ID={}, SKU={}", product.getId(), product.getSku());
        }
    }

    /**
     * Fetch category map for batch loading.
     */
    private Map<String, Category> fetchCategoryMap(List<Products> products) {
        List<String> categoryIds = products.stream()
                .map(Products::getCategoryId)
                .distinct()
                .toList();

        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    /**
     * Fetch UOM map for batch loading.
     */
    private Map<String, UnitsOfMeasure> fetchUomMap(List<Products> products) {
        List<String> uomIds = products.stream()
                .map(Products::getUomId)
                .distinct()
                .toList();

        return unitsOfMeasureRepository.findAllById(uomIds).stream()
                .collect(Collectors.toMap(UnitsOfMeasure::getId, u -> u));
    }

    /**
     * Set audit fields for create or update.
     */
    private void setAuditFields(Products product, Account account, boolean isCreate) {
        LocalDateTime now = LocalDateTime.now();

        if (isCreate) {
            product.setCreatedBy(account.getId());
            product.setCreatedAt(now);
        }

        product.setUpdatedBy(account.getId());
        product.setUpdatedAt(now);
    }

    /**
     * Get current authenticated user.
     */
    private Account getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", ErrorCode.AUTH_002));
    }
}


