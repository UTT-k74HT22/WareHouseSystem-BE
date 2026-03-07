package org.demo.whs.repository;

import org.demo.whs.entity.BusinessPartners;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for BusinessPartners entity.
 */
@Repository
public interface BusinessPartnersRepository extends JpaRepository<BusinessPartners, String> {

    /**
     * Checks if a business partner exists by its code.
     *
     * @param code the code of the business partner to check
     * @return true if a business partner with the given code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Checks if an active supplier exists by its unique identifier.
     *
     * @param id the unique identifier of the supplier to check
     * @return true if an active supplier with the given id exists, false otherwise
     */
    @Query("""
        SELECT COUNT(bp) > 0
        FROM BusinessPartners bp
        WHERE bp.id = :id
          AND bp.status = org.demo.whs.entity.enums.BusinessPartnerStatus.ACTIVE
          AND bp.type IN (
              org.demo.whs.entity.enums.BusinessPartnerType.SUPPLIER,
              org.demo.whs.entity.enums.BusinessPartnerType.BOTH
          )
    """)
    boolean existsActiveSupplierById(String id);
}
