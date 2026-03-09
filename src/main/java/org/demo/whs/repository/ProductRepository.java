package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Products entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Products, String> {

    /**
     * Check if a product exists by SKU (case-insensitive).
     *
     * @param sku the product SKU
     * @return true if product exists, false otherwise
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Find a product by SKU (case-insensitive).
     *
     * @param sku the product SKU
     * @return optional product
     */
    Optional<Products> findBySkuIgnoreCase(String sku);

    /**
     * Check if a product exists by SKU (case-insensitive) excluding a specific ID.
     * Used for update validation.
     *
     * @param sku the product SKU
     * @param id  the product ID to exclude
     * @return true if product exists, false otherwise
     */
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, String id);

    /**
     * Search products with dynamic filters.
     *
     * @param sku                     filter by SKU (exact match, case-insensitive)
     * @param name                    filter by name (partial match, case-insensitive)
     * @param categoryId              filter by category ID
     * @param uomId                   filter by UOM ID
     * @param status                  filter by status
     * @param requiresBatchTracking   filter by batch tracking requirement
     * @param pageable                pagination information
     * @return page of products
     */
    @Query("SELECT p FROM Products p WHERE " +
           "(:sku IS NULL OR LOWER(p.sku) = LOWER(:sku)) AND " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:uomId IS NULL OR p.uomId = :uomId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:requiresBatchTracking IS NULL OR p.requiresBatchTracking = :requiresBatchTracking)")
    Page<Products> searchProducts(
            @Param("sku") String sku,
            @Param("name") String name,
            @Param("categoryId") String categoryId,
            @Param("uomId") String uomId,
            @Param("status") ProductStatus status,
            @Param("requiresBatchTracking") Boolean requiresBatchTracking,
            Pageable pageable
    );

    /**
     * Count products by category ID.
     *
     * @param categoryId the category ID
     * @return count of products
     */
    long countByCategoryId(String categoryId);

    /**
     * Count products by UOM ID.
     *
     * @param uomId the UOM ID
     * @return count of products
     */
    long countByUomId(String uomId);

    /**
     * Find a product by ID with a pessimistic write lock for update operations.
     *
     * @param id the product ID
     * @return optional product with lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Products p WHERE p.id = :id")
    Optional<PurchaseOrders> findByIdForUpdate(String id);
}


