package org.demo.whs.repository.specification;

import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PurchaseOrdersSpecification {

    private PurchaseOrdersSpecification() {
    }

    public static Specification<PurchaseOrders> withFilter(PurchaseOrdersFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            if (StringUtils.hasText(filter.getPurchaseOrderNumber())) {
                predicates = cb.and(
                        predicates,
                        cb.like(
                                cb.lower(root.get("purchaseOrderNumber")),
                                "%" + filter.getPurchaseOrderNumber().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(filter.getSupplierId())) {
                predicates = cb.and(predicates, cb.equal(root.get("supplierId"), filter.getSupplierId().trim()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates = cb.and(predicates, cb.equal(root.get("warehouseId"), filter.getWarehouseId().trim()));
            }

            if (StringUtils.hasText(filter.getStatus())) {
                predicates = cb.and(
                        predicates,
                        cb.equal(root.get("status"), PurchaseOrdersStatus.valueOf(filter.getStatus()))
                );
            }

            if (filter.getOrderDateFrom() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("orderDate"), filter.getOrderDateFrom()));
            }

            if (filter.getOrderDateTo() != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("orderDate"), filter.getOrderDateTo()));
            }

            if (filter.getExpectedDeliveryDateFrom() != null) {
                predicates = cb.and(
                        predicates,
                        cb.greaterThanOrEqualTo(root.get("expectedDeliveryDate"), filter.getExpectedDeliveryDateFrom())
                );
            }

            if (filter.getExpectedDeliveryDateTo() != null) {
                predicates = cb.and(
                        predicates,
                        cb.lessThanOrEqualTo(root.get("expectedDeliveryDate"), filter.getExpectedDeliveryDateTo())
                );
            }

            return predicates;
        };
    }
}
