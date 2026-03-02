package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.JoinType;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class InventorySpecification {

    public static Specification<Inventory> withFilter(InventoryFilterRequest filter) {
        return (root, query, cb) -> {
            // Fetch joins to avoid N+1
            if (Long.class != query.getResultType()) { // Avoid fetch for count query
                root.fetch("product", JoinType.LEFT);
                root.fetch("warehouse", JoinType.LEFT);
                root.fetch("location", JoinType.LEFT);
                root.fetch("batch", JoinType.LEFT);
            }

            var predicates = cb.conjunction();

            if (StringUtils.hasText(filter.getProductId())) {
                predicates = cb.and(predicates, cb.equal(root.get("productId"), filter.getProductId()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates = cb.and(predicates, cb.equal(root.get("warehouseId"), filter.getWarehouseId()));
            }

            if (StringUtils.hasText(filter.getLocationId())) {
                predicates = cb.and(predicates, cb.equal(root.get("locationId"), filter.getLocationId()));
            }

            if (StringUtils.hasText(filter.getBatchId())) {
                predicates = cb.and(predicates, cb.equal(root.get("batchId"), filter.getBatchId()));
            }

            if (StringUtils.hasText(filter.getProductSku())) {
                var productJoin = root.join("product", JoinType.LEFT);
                predicates = cb.and(predicates, cb.like(cb.lower(productJoin.get("sku")), 
                        "%" + filter.getProductSku().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(filter.getProductName())) {
                var productJoin = root.join("product", JoinType.LEFT);
                predicates = cb.and(predicates, cb.like(cb.lower(productJoin.get("name")), 
                        "%" + filter.getProductName().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(filter.getBatchNumber())) {
                var batchJoin = root.join("batch", JoinType.LEFT);
                predicates = cb.and(predicates, cb.like(cb.lower(batchJoin.get("batchNumber")), 
                        "%" + filter.getBatchNumber().toLowerCase() + "%"));
            }

            return predicates;
        };
    }
}
