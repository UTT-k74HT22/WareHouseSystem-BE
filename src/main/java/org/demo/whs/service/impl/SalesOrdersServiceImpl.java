package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.SalesOrdersRepository;
import org.demo.whs.service.SalesOrdersService;
import org.springframework.stereotype.Service;

/**
 * Implementation of the SalesOrdersService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrdersServiceImpl implements SalesOrdersService {
    private final SalesOrdersRepository salesOrdersRepository;
}
