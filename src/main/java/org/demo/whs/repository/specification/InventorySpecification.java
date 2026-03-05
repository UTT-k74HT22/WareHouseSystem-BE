package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.Subquery;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class InventorySpecification {

    public static Specification<Inventory> withFilter(InventoryFilterRequest filter) {
        return (root, query, cb) -> {
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

            // Filter by Product SKU/Name using Subquery
            if (StringUtils.hasText(filter.getProductSku()) || StringUtils.hasText(filter.getProductName())) {
                Subquery<String> productSubquery = query.subquery(String.class);
                var productRoot = productSubquery.from(Products.class);
                productSubquery.select(productRoot.get("id"));
                
                var productPredicates = cb.conjunction();
                if (StringUtils.hasText(filter.getProductSku())) {
                    productPredicates = cb.and(productPredicates, 
                        cb.like(cb.lower(productRoot.get("sku")), "%" + filter.getProductSku().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(filter.getProductName())) {
                    productPredicates = cb.and(productPredicates, 
                        cb.like(cb.lower(productRoot.get("name")), "%" + filter.getProductName().toLowerCase() + "%"));
                }
                productSubquery.where(productPredicates);
                
                predicates = cb.and(predicates, root.get("productId").in(productSubquery));
            }

            // Filter by Batch Number using Subquery
            if (StringUtils.hasText(filter.getBatchNumber())) {
                Subquery<String> batchSubquery = query.subquery(String.class);
                var batchRoot = batchSubquery.from(Batch.class);
                batchSubquery.select(batchRoot.get("id"));
                
                batchSubquery.where(cb.like(cb.lower(batchRoot.get("batchNumber")), 
                    "%" + filter.getBatchNumber().toLowerCase() + "%"));
                
                predicates = cb.and(predicates, root.get("batchId").in(batchSubquery));
            }

            return predicates;
        };
    }
}
