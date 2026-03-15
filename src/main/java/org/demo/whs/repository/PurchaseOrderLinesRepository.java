package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.PurchaseOrderLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderLinesRepository extends JpaRepository<PurchaseOrderLines, String> {

    boolean existsByPurchaseOrderId(String purchaseOrderId);

    long countByPurchaseOrderId(String purchaseOrderId);

    List<PurchaseOrderLines> findByPurchaseOrderIdOrderByLineNumberAsc(String purchaseOrderId);

    Optional<PurchaseOrderLines> findTopByPurchaseOrderIdOrderByLineNumberDesc(String purchaseOrderId);

    boolean existsByPurchaseOrderIdAndProductId(String purchaseOrderId, String productId);

    boolean existsByPurchaseOrderIdAndProductIdAndIdNot(String purchaseOrderId, String productId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pol from PurchaseOrderLines pol where pol.id = :id")
    Optional<PurchaseOrderLines> findByIdForUpdate(@Param("id") String id);

    @Query(value = "SELECT * FROM purchase_order_lines WHERE purchase_order_id = :purchaseOrderId FOR UPDATE", nativeQuery = true)
    List<PurchaseOrderLines> findByPurchaseOrderIdForUpdate(String purchaseOrderId);
}
