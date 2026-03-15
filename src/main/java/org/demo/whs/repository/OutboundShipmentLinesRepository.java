package org.demo.whs.repository;

import org.demo.whs.entity.OutboundShipmentLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OutboundShipmentLines entity.
 */
@Repository
public interface OutboundShipmentLinesRepository extends JpaRepository<OutboundShipmentLines, String> {

    List<OutboundShipmentLines> findByBatchIdOrderByCreatedAtDesc(String batchId);
}
