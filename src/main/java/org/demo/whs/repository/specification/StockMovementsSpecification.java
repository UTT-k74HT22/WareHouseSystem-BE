package org.demo.whs.repository.specification;

import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.StockMovements.SearchStockMovementsRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StockMovementsSpecification {

    private StockMovementsSpecification() {
    }

    public static Specification<StockMovements> withFilter(SearchStockMovementsRequest filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            if (StringUtils.hasText(filter.getProductId())) {
                predicates = cb.and(predicates, cb.equal(root.get("productId"), filter.getProductId().trim()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates = cb.and(predicates, cb.equal(root.get("warehouseId"), filter.getWarehouseId().trim()));
            }

            if (StringUtils.hasText(filter.getLocationId())) {
                predicates = cb.and(predicates, cb.or(
                        cb.equal(root.get("locationId"), filter.getLocationId().trim()),
                        cb.equal(root.get("toLocationId"), filter.getLocationId().trim())
                ));
            }

            if (StringUtils.hasText(filter.getBatchId())) {
                predicates = cb.and(predicates, cb.equal(root.get("batchId"), filter.getBatchId().trim()));
            }

            if (filter.getMovementType() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("movementType"), filter.getMovementType()));
            }

            if (filter.getReferenceType() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("referenceType"), filter.getReferenceType()));
            }

            if (filter.getMovementDateFrom() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(
                        root.get("movementDate"), filter.getMovementDateFrom().atStartOfDay()));
            }

            if (filter.getMovementDateTo() != null) {
                predicates = cb.and(predicates, cb.lessThan(
                        root.get("movementDate"), filter.getMovementDateTo().plusDays(1).atStartOfDay()));
            }

            return predicates;
        };
    }
}
