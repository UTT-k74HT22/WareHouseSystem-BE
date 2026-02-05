package org.demo.whs.repository;

import org.demo.whs.entity.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Purchase Orders.
 */
@Repository
public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, String> {
}
