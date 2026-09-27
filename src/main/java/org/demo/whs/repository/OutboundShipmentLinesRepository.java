package org.demo.whs.repository;

import org.demo.whs.entity.OutboundShipmentLines;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for OutboundShipmentLines entity.
 */
@Repository
public interface OutboundShipmentLinesRepository extends JpaRepository<OutboundShipmentLines, String> {
    List<OutboundShipmentLines> findByOutboundShipmentId(String outboundShipmentId);

    List<OutboundShipmentLines> findByBatchIdOrderByCreatedAtDesc(String batchId);

    @Query("select case when count(osl) > 0 then true else false end from OutboundShipmentLines osl " +
           "where osl.outboundShipmentId = :shipmentId " +
           "and osl.salesOrderLineId = :soLineId " +
           "and ((:locationId is null and osl.locationId is null) or osl.locationId = :locationId) " +
           "and ((:batchId is null and osl.batchId is null) or osl.batchId = :batchId) " +
           "and (:excludeLineId is null or osl.id <> :excludeLineId)")
    boolean existsByDuplicateDimension(
            @Param("shipmentId") String shipmentId,
            @Param("soLineId") String soLineId,
            @Param("locationId") String locationId,
            @Param("batchId") String batchId,
            @Param("excludeLineId") String excludeLineId);

    @Query("SELECT MAX(l.lineNumber) FROM OutboundShipmentLines l WHERE l.outboundShipmentId = :shipmentId")
    Integer findMaxLineNumber(@Param("shipmentId") String shipmentId);

    @Query("SELECT SUM(osl.quantityShipped) FROM OutboundShipmentLines osl WHERE osl.outboundShipmentId = :shipmentId AND osl.salesOrderLineId = :soLineId")
    BigDecimal sumShippedForSoLine(@Param("shipmentId") String shipmentId, @Param("soLineId") String soLineId);

    @Query("""
        SELECT COALESCE(SUM(osl.quantityShipped), 0)
        FROM OutboundShipmentLines osl, OutboundShipments os
        WHERE os.id = osl.outboundShipmentId
          AND osl.locationId = :locationId
          AND os.status IN :statuses
        """)
    BigDecimal sumQuantityByLocationIdAndShipmentStatuses(@Param("locationId") String locationId,
                                                          @Param("statuses") List<OutboundShipmentsStatus> statuses);
}
