package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Get inventories with pagination and filtering
     * @param filter InventoryFilterRequest
     * @param pageable Pageable
     * @return PageResponse<InventoryResponse>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventories(InventoryFilterRequest filter, Pageable pageable) {
        log.info("Fetching inventories with filter: {}, pageable: {}", filter, pageable);
        validatePageable(pageable);
        Page<Inventory> inventoryPage = inventoryRepository.findAll(
                InventorySpecification.withFilter(filter),
                pageable
        );

        return PageResponse.from(inventoryPage, inventoryMapper.toResponses(inventoryPage.getContent()));
    }
                    // Private method
    private void validatePageable(Pageable pageable) {

        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException(ErrorCode.COM_006.getMessage());
        }

        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException(ErrorCode.COM_007.getMessage());
        }

        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException(ErrorCode.COM_008.getMessage());
        }
    }
}
