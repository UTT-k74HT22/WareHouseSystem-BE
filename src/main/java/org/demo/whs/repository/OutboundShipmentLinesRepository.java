package org.demo.whs.repository;

import org.demo.whs.entity.OutboundShipmentLines;
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

    List<OutboundShipmentLines> findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
            String outboundShipmentId, String salesOrderLineId, String locationId, String batchId);

    @Query("SELECT MAX(l.lineNumber) FROM OutboundShipmentLines l WHERE l.outboundShipmentId = :shipmentId")
    Integer findMaxLineNumber(@Param("shipmentId") String shipmentId);

    @Query("SELECT SUM(osl.quantityShipped) FROM OutboundShipmentLines osl WHERE osl.outboundShipmentId = :shipmentId AND osl.salesOrderLineId = :soLineId")
    BigDecimal sumShippedForSoLine(@Param("shipmentId") String shipmentId, @Param("soLineId") String soLineId);
}
