package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.service.BusinessPartnerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling Business Partner APIs.
 * API Version: v1
 */
@RestController
@RequestMapping("/api/v1/business-partners")
@RequiredArgsConstructor
@Slf4j
@Validated
public class BusinessPartnerController {

    private final BusinessPartnerService businessPartnerService;

    /* ================= CREATE ================= */

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BusinessPartnerResponse>> create(
            @RequestBody @Valid BusinessPartnerRequest request
    ) {
        log.debug("Create business partner request, code={}", request.getCode());

        BusinessPartnerResponse response =
                businessPartnerService.create(request);

        log.info("Business partner created successfully, id={}, code={}",
                response.getId(), response.getCode());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    /* ================= GET ALL ================= */

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<BaseResponse<List<BusinessPartnerResponse>>> getAll() {
        log.debug("Get all business partners request");

        List<BusinessPartnerResponse> responses =
                businessPartnerService.getAll();

        log.info("Fetched {} business partners", responses.size());

        return ResponseEntity.ok(BaseResponse.success(responses));
    }

    /* ================= GET BY ID ================= */

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<BaseResponse<BusinessPartnerResponse>> getById(
            @PathVariable String id
    ) {
        log.debug("Get business partner by id={}", id);

        BusinessPartnerResponse response =
                businessPartnerService.getById(id);

        log.info("Fetched business partner successfully, id={}, code={}",
                response.getId(), response.getCode());

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /* ================= UPDATE ================= */

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BusinessPartnerResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateBusinessPartnerRequest request
    ) {
        log.debug("Update business partner request, id={}", id);

        BusinessPartnerResponse response =
                businessPartnerService.update(id, request);

        log.info("Business partner updated successfully, id={}, code={}",
                response.getId(), response.getCode());

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /* ================= DELETE ================= */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable String id
    ) {
        log.debug("Delete business partner request, id={}", id);

        businessPartnerService.delete(id);

        log.info("Business partner deleted successfully (soft delete), id={}", id);

        return ResponseEntity.ok(BaseResponse.success(null));
    }

    /* ================= CHANGE STATUS ================= */

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BusinessPartnerResponse>> changeStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        log.debug("Change business partner status, id={}, status={}", id, status);

        BusinessPartnerResponse response =
                businessPartnerService.changeStatus(id, status);

        log.info("Business partner status changed successfully, id={}, newStatus={}",
                response.getId(), response.getStatus());

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
