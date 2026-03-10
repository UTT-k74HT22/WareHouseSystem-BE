package org.demo.whs.repository.specification;

import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class InboundReceiptsSpecification {

    private InboundReceiptsSpecification() {
    }

    public static Specification<InboundReceipts> withFilter(InboundReceiptsFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (filter == null) {
                return predicates;
            }

            if (StringUtils.hasText(filter.getReceiptNumber())) {
                predicates = cb.and(
                        predicates,
                        cb.like(
                                cb.lower(root.get("receiptNumber")),
                                "%" + filter.getReceiptNumber().trim().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(filter.getPurchaseOrderId())) {
                predicates = cb.and(predicates, cb.equal(root.get("purchaseOrderId"), filter.getPurchaseOrderId().trim()));
            }

            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates = cb.and(predicates, cb.equal(root.get("warehouseId"), filter.getWarehouseId().trim()));
            }

            if (StringUtils.hasText(filter.getStatus())) {
                predicates = cb.and(
                        predicates,
                        cb.equal(root.get("status"), InboundReceiptsStatus.valueOf(filter.getStatus()))
                );
            }

            if (filter.getReceiptDateFrom() != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("receiptDate"), filter.getReceiptDateFrom()));
            }

            if (filter.getReceiptDateTo() != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("receiptDate"), filter.getReceiptDateTo()));
            }

            return predicates;
        };
    }
}
