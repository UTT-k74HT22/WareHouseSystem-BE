package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.InboundReceipts;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Inbound Receipts.
 */
@Repository
public interface InboundReceiptsRepository extends JpaRepository<InboundReceipts, String>, JpaSpecificationExecutor<InboundReceipts> {

    boolean existsByReceiptNumber(String receiptNumber);

    List<InboundReceipts> findByPurchaseOrderIdOrderByCreatedAtDesc(String purchaseOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ir from InboundReceipts ir where ir.id = :id")
    Optional<InboundReceipts> findByIdForUpdate(@Param("id") String id);
}
