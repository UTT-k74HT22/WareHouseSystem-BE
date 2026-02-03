package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.service.PurchaseOrdersService;
import org.springframework.stereotype.Service;

/**
 * Implementation of the PurchaseOrdersService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrdersServiceImpl implements PurchaseOrdersService {
    private final PurchaseOrdersRepository purchaseOrdersRepository;
}
