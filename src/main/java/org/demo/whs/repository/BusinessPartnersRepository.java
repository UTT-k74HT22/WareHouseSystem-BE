package org.demo.whs.repository;

import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for BusinessPartners entity.
 */
@Repository
public interface BusinessPartnersRepository extends JpaRepository<BusinessPartners, String> {
    /**
     * */
    boolean existsByCode(String code);


}
