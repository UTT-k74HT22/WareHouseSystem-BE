package org.demo.whs.repository;

import org.demo.whs.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    
    Optional<InventoryReservation> findByRequestKey(String requestKey);
    
    Optional<InventoryReservation> findByOrderLineId(String orderLineId);
}
