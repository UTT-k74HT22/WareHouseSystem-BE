package org.demo.whs.repository;

import org.demo.whs.entity.InboundReceiptLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing inbound receipt lines.
 */
@Repository
public interface InboundReceiptLinesRepository extends JpaRepository<InboundReceiptLines, String> {
}
