package org.demo.whs.repository;

import org.demo.whs.entity.UnitsOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UnitsOfMeasure entity.
 */
@Repository
public interface UnitsOfMeasureRepository extends JpaRepository<UnitsOfMeasure, String> {
}
