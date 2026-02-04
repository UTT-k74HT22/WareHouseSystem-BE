package org.demo.whs.repository;

import org.demo.whs.entity.StockMovements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for StockMovements entity.
 */
@Repository
public interface StockMovementsRepository extends JpaRepository<StockMovements, String> {
}
