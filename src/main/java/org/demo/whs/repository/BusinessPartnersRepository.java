package org.demo.whs.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

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

    @Query("""
        SELECT bp
        FROM BusinessPartners bp
        WHERE (:code IS NULL OR LOWER(bp.code) LIKE LOWER(CONCAT('%', :code, '%')))
        AND (:name IS NULL OR LOWER(bp.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:status IS NULL OR bp.status = :status)
        AND (:types IS NULL OR bp.type IN :types)
    """)
    Page<BusinessPartners> search(
            @Param("code") String code,
            @Param("name") String name,
            @Param("status") BusinessPartnerStatus status,
            @Param("types") List<BusinessPartnerType> types,
            Pageable pageable
    );
}
