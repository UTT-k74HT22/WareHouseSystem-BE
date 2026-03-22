package org.demo.whs.repository;

import org.demo.whs.entity.BackgroundJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository interface for managing BackgroundJob entities.
 */
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, String> {

    /**
     * Finds a page of BackgroundJob entities requested by a specific user, ordered by creation date in descending order.
     *
     * @param requestedBy the username of the requester
     * @param pageable    the pagination information
     * @return a page of BackgroundJob entities
     */
    Page<BackgroundJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);

    /**
     * Finds a BackgroundJob entity by its ID and the username of the requester.
     *
     * @param id          the ID of the BackgroundJob
     * @param requestedBy the username of the requester
     * @return an Optional containing the found BackgroundJob, or empty if not found
     */
    Optional<BackgroundJob> findByIdAndRequestedBy(String id, String requestedBy);
}
