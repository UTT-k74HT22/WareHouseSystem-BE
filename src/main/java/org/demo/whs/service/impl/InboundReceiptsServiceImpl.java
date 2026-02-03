package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.stereotype.Service;

/**
 * Implementation of the InboundReceiptsService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptsServiceImpl implements InboundReceiptsService {
    private final InboundReceiptsRepository inboundReceiptsRepository;
}
