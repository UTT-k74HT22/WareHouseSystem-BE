package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SalesOrdersSpecification {

    private SalesOrdersSpecification() {
    }

    public static Specification<SalesOrders> withFilter(SalesOrdersFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return cb.conjunction();
            }

            if (StringUtils.hasText(filter.getSoNumber())) {
                predicates.add(cb.like(
                        cb.lower(root.get("soNumber")),
                        "%" + filter.getSoNumber().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(filter.getCustomerId())) {
                predicates.add(cb.equal(root.get("customerId"), filter.getCustomerId().trim()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates.add(cb.equal(root.get("warehouseId"), filter.getWarehouseId().trim()));
            }

            if (StringUtils.hasText(filter.getStatus())) {
                try {
                    predicates.add(cb.equal(root.get("status"), SalesOrdersStatus.valueOf(filter.getStatus().toUpperCase())));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            if (filter.getOrderDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), filter.getOrderDateFrom()));
            }

            if (filter.getOrderDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), filter.getOrderDateTo()));
            }

            if (filter.getRequestedDeliveryDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedDeliveryDate"), filter.getRequestedDeliveryDateFrom()));
            }

            if (filter.getRequestedDeliveryDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestedDeliveryDate"), filter.getRequestedDeliveryDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
