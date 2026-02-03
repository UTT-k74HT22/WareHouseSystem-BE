package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.StockAdjustmentsRepository;
import org.demo.whs.service.StockAdjustmentsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAdjustmentsServiceImpl implements StockAdjustmentsService {
    private final StockAdjustmentsRepository stockAdjustmentsRepository;
}
