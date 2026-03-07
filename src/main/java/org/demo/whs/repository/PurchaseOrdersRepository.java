package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import org.demo.whs.entity.PurchaseOrders;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing Purchase Orders.
 */
@Repository
public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, String>, JpaSpecificationExecutor<PurchaseOrders> {

    boolean existsByPurchaseOrderNumber(String purchaseOrderNumber);

    Optional<PurchaseOrders> findByPurchaseOrderNumber(String purchaseOrderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PurchaseOrders po where po.id = :id")
    Optional<PurchaseOrders> findByIdForUpdate(@Param("id") String id);
}
