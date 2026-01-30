package org.demo.whs.repository;

import org.demo.whs.entity.Locations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Location entity.
 */
@Repository
public interface LocationRepository extends JpaRepository<Locations, String> {
}
