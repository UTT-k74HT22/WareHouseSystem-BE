package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.enums.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InboundReceiptLinesRepository extends JpaRepository<InboundReceiptLines, String> {

    List<InboundReceiptLines> findByInboundReceiptIdOrderByLineNumberAsc(String inboundReceiptId);

    List<InboundReceiptLines> findByBatchIdOrderByCreatedAtDesc(String batchId);

    @Query(value = "SELECT COUNT(*) FROM inbound_receipt_lines WHERE inbound_receipt_id = :inboundReceiptId", nativeQuery = true)
    long countByInboundReceiptId(@Param("inboundReceiptId") String inboundReceiptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select irl from InboundReceiptLines irl where irl.id = :id")
    Optional<InboundReceiptLines> findByIdForUpdate(@Param("id") String id);

    Optional<InboundReceiptLines> findTopByInboundReceiptIdOrderByLineNumberDesc(String inboundReceiptId);

    @Query("select COALESCE(sum(irl.quantityReceived), 0) " +
           "from InboundReceiptLines irl " +
           "where irl.inboundReceiptId = :inboundReceiptId " +
           "and irl.purchaseOrderLineId = :purchaseOrderLineId " +
           "and (:excludeLineId is null or irl.id <> :excludeLineId)")
    BigDecimal sumQuantityByReceiptAndPurchaseOrderLine(
            @Param("inboundReceiptId") String inboundReceiptId,
            @Param("purchaseOrderLineId") String purchaseOrderLineId,
            @Param("excludeLineId") String excludeLineId);

    @Query("select case when count(irl) > 0 then true else false end from InboundReceiptLines irl " +
           "where irl.inboundReceiptId = :inboundReceiptId " +
           "and irl.purchaseOrderLineId = :purchaseOrderLineId " +
           "and irl.locationId = :locationId " +
           "and ((:batchId is null and irl.batchId is null) or irl.batchId = :batchId) " +
           "and irl.qualityStatus = :qualityStatus " +
           "and (:excludeLineId is null or irl.id <> :excludeLineId)")
    boolean existsByDuplicateDimension(
            @Param("inboundReceiptId") String inboundReceiptId,
            @Param("purchaseOrderLineId") String purchaseOrderLineId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId,
            @Param("qualityStatus") QualityStatus qualityStatus,
            @Param("excludeLineId") String excludeLineId);
}
