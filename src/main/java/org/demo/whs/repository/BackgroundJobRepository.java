package org.demo.whs.repository;

import org.demo.whs.entity.BackgroundJob;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing BackgroundJob entities.
 */
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, String> {
}
