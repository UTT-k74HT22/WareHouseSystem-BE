package org.demo.whs.repository;

import org.demo.whs.entity.SalesOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for SalesOrders entity.
 */
@Repository
public interface SalesOrdersRepository extends JpaRepository<SalesOrders, String> {
}
