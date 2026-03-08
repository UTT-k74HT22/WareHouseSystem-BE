package org.demo.whs.repository;

import org.demo.whs.entity.PurchaseOrderLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLinesRepository extends JpaRepository<PurchaseOrderLines, String> {

    boolean existsByPurchaseOrderId(String purchaseOrderId);

    long countByPurchaseOrderId(String purchaseOrderId);

    List<PurchaseOrderLines> findByPurchaseOrderIdOrderByLineNumberAsc(String purchaseOrderId);
}
