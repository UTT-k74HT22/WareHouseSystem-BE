package org.demo.whs.repository;

import org.demo.whs.entity.BusinessPartners;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for BusinessPartners entity.
 */
@Repository
public interface BusinessPartnersRepository extends JpaRepository<BusinessPartners, String> {
}
