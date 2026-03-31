package org.demo.whs.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.demo.whs.entity.OutboundShipments;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OutboundShipmentsSpecification {

    public static Specification<OutboundShipments> withFilter(OutboundShipmentsFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getShipmentNumber() != null && !filter.getShipmentNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("shipmentNumber")), "%" + filter.getShipmentNumber().toLowerCase() + "%"));
            }

            if (filter.getSalesOrderId() != null && !filter.getSalesOrderId().isBlank()) {
                predicates.add(cb.equal(root.get("salesOrderId"), filter.getSalesOrderId()));
            }

            if (filter.getWarehouseId() != null && !filter.getWarehouseId().isBlank()) {
                predicates.add(cb.equal(root.get("warehouseId"), filter.getWarehouseId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getShipmentDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("shipmentDate"), filter.getShipmentDateFrom()));
            }

            if (filter.getShipmentDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("shipmentDate"), filter.getShipmentDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
