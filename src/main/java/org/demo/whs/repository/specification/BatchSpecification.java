package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.Subquery;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.dto.request.Batch.SearchBatchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class BatchSpecification {

    private BatchSpecification() {
    }

    public static Specification<Batch> withFilter(SearchBatchRequest filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            if (StringUtils.hasText(filter.getKeyword())) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("batchNumber")), keyword),
                        cb.like(cb.lower(cb.coalesce(root.get("supplierBatchNumber"), "")), keyword)
                ));
            }

            if (StringUtils.hasText(filter.getProductId())) {
                predicates = cb.and(predicates, cb.equal(root.get("productId"), filter.getProductId().trim()));
            }

            if (filter.getStatus() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getManufacturingDateFrom() != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("manufacturingDate"), filter.getManufacturingDateFrom()));
            }

            if (filter.getManufacturingDateTo() != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("manufacturingDate"), filter.getManufacturingDateTo()));
            }

            if (filter.getExpiryDateFrom() != null) {
                predicates = cb.and(predicates,
                        cb.isNotNull(root.get("expiryDate")),
                        cb.greaterThanOrEqualTo(root.get("expiryDate"), filter.getExpiryDateFrom()));
            }

            if (filter.getExpiryDateTo() != null) {
                predicates = cb.and(predicates,
                        cb.isNotNull(root.get("expiryDate")),
                        cb.lessThanOrEqualTo(root.get("expiryDate"), filter.getExpiryDateTo()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                Subquery<String> inventorySubquery = query.subquery(String.class);
                var inventoryRoot = inventorySubquery.from(Inventory.class);
                inventorySubquery.select(inventoryRoot.get("batchId"))
                        .where(cb.equal(inventoryRoot.get("warehouseId"), filter.getWarehouseId().trim()));
                predicates = cb.and(predicates, root.get("id").in(inventorySubquery));
            }

            return predicates;
        };
    }
}
