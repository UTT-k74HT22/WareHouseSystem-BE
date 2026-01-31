package org.demo.whs.service;

import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.demo.whs.entity.dto.response.PageResponse;

public interface AccountService {
    PageResponse<AccountResponse> getList(int page, int size);
}
