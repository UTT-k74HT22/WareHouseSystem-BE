package org.demo.whs.repository;

import org.demo.whs.entity.OutboundShipments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OutboundShipments entity.
 */
@Repository
public interface OutboundShipmentsRepository extends JpaRepository<OutboundShipments, String> {
    List<OutboundShipments> findBySalesOrderId(String salesOrderId);
}
