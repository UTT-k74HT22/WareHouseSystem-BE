package org.demo.whs.repository;

import org.demo.whs.entity.StockAdjustments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing stock adjustments data.
 */
@Repository
public interface StockAdjustmentsRepository extends JpaRepository<StockAdjustments, String> {
}
