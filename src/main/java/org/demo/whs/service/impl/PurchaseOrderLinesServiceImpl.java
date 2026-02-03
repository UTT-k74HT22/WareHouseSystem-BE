package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.service.PurchaseOrderLinesService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing purchase order lines.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderLinesServiceImpl implements PurchaseOrderLinesService {
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
}
