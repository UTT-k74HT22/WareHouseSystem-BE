package org.demo.whs.repository;

import org.demo.whs.entity.StockTransfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing stock transfers data.
 */
@Repository
public interface StockTransfersRepository extends JpaRepository<StockTransfers, String> {
}
