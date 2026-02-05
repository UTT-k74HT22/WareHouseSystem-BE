package org.demo.whs.repository;

import org.demo.whs.entity.UnitsOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UnitsOfMeasure entity.
 */
@Repository
public interface UnitsOfMeasureRepository extends JpaRepository<UnitsOfMeasure, String> {

    /**
     * Checks if a unit of measure exists by its code.
     *
     * @param code the code of the unit of measure
     * @return true if a unit of measure with the given code exists, false otherwise
     */
    Boolean existsUnitsOfMeasureByCode(String code);
}
