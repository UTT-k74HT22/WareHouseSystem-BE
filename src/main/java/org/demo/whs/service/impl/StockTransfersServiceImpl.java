package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.StockTransfersRepository;
import org.demo.whs.service.StockTransfersService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockTransfersServiceImpl implements StockTransfersService {

    private final StockTransfersRepository stockTransfersRepository;

}
