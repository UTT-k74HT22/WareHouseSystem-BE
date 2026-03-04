package org.demo.whs.service;


import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;

public interface BusinessPartnerService {

    /**
     * Lấy danh sách đối tác
     */
    PageResponse<BusinessPartnerResponse> getAll(Integer page, Integer size, String sortBy, String sortDir);

    /**
     * Lấy chi tiết theo id
     */
    BusinessPartnerResponse getById(String id);

    /**
     * Tạo mới đối tác
     */
    BusinessPartnerResponse create(BusinessPartnerRequest request);

    /**
     * Cập nhật đối tác
     */
    BusinessPartnerResponse update(String id, UpdateBusinessPartnerRequest request);

    /**
     * Xoá đối tác (soft delete hoặc hard delete tuỳ bạn)
     */
    void delete(String id);

    /**
     * Đổi trạng thái (optional nhưng rất hay dùng)
     */
    BusinessPartnerResponse changeStatus(String id, String status);
}
