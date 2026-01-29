package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.ProductStatus;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Products extends BaseEntity {

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id", nullable = false, columnDefinition = "char(36)")
    private String categoryId;

    @Column(name = "uom_id", nullable = false, columnDefinition = "char(36)")
    private String uomId;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "dimensions", length = 100)
    private String dimensions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @Column(name = "min_stock_level")
    private Double minStockLevel;

    @Column(name = "max_stock_level")
    private Double maxStockLevel;

    @Column(name = "reorder_point")
    private Double reOrderPoint;

    @Column(name = "cost_price")
    private Double costPrice;

    @Column(name = "selling_price")
    private Double sellingPrice;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "requires_batch_tracking")
    private Boolean requiresBatchTracking;
}
