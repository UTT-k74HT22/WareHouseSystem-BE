package org.demo.whs.service;

import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.demo.whs.entity.dto.response.PageResponse;

public interface AccountService {

    /**
     * Retrieves a paginated list of accounts.
     *
     * @param page the page number
     * @param size the page size
     * @return paginated account response
     */
    PageResponse<AccountResponse> getList(int page, int size);
}
