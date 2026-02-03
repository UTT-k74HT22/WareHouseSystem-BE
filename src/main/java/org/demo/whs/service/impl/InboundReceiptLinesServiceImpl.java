package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.service.InboundReceiptLinesService;
import org.springframework.stereotype.Service;

/**
 * Implementation of the service for managing inbound receipt lines.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptLinesServiceImpl implements InboundReceiptLinesService {
    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
}
