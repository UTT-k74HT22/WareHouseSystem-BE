package org.demo.whs.repository;

import org.demo.whs.entity.PurchaseOrderLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderLinesRepository extends JpaRepository<PurchaseOrderLines, String>
{
}
