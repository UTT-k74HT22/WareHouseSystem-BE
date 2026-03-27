package org.demo.whs.repository;

import org.demo.whs.entity.InventoryReservation;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    Optional<InventoryReservation> findByOrderLineId(String orderLineId);

    @Query("""
        SELECT COALESCE(SUM(ir.quantity), 0)
        FROM InventoryReservation ir
        WHERE ir.inventoryId = :inventoryId
          AND ir.status = :status
        """)
    BigDecimal sumQuantityByInventoryIdAndStatus(@Param("inventoryId") String inventoryId,
                                                 @Param("status") InventoryReservationStatus status);

}
