package org.demo.whs.repository;

import org.demo.whs.entity.InboundReceipts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Inbound Receipts.
 */
@Repository
public interface InboundReceiptsRepository extends JpaRepository<InboundReceipts, String> {
}
