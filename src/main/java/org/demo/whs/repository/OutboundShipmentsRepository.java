package org.demo.whs.repository;

import org.demo.whs.entity.OutboundShipments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OutboundShipments entity.
 */
@Repository
public interface OutboundShipmentsRepository extends JpaRepository<OutboundShipments, String>, JpaSpecificationExecutor<OutboundShipments> {
    List<OutboundShipments> findBySalesOrderId(String salesOrderId);
    boolean existsByShipmentNumber(String shipmentNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OutboundShipments s WHERE s.id = :id")
    Optional<OutboundShipments> findByIdWithLock(@Param("id") String id);
}
