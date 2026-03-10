package org.demo.whs.repository;

import org.demo.whs.entity.InboundReceiptLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing inbound receipt lines.
 */
@Repository
public interface InboundReceiptLinesRepository extends JpaRepository<InboundReceiptLines, String> {

    /**
     * Find all lines associated with a given inbound receipt ID, ordered by line number in ascending order.
     *
     * @param inboundReceiptId the ID of the inbound receipt
     * @return a list of lines associated with the specified inbound receipt ID, ordered by line number
     */
    List<InboundReceiptLines> findByInboundReceiptIdOrderByLineNumberAsc(String inboundReceiptId);

    /**
     * Count the number of lines for a given inbound receipt ID.
     *
     * @param inboundReceiptId the ID of the inbound receipt
     * @return the count of lines associated with the specified inbound receipt ID
     */
    @Query(value = "SELECT COUNT(*) FROM inbound_receipt_lines WHERE inbound_receipt_id = :inboundReceiptId", nativeQuery = true)
    long countByInboundReceiptId(String inboundReceiptId);
}
