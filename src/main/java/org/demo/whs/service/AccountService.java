package org.demo.whs.service;

import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.demo.whs.entity.dto.response.PageResponse;

import java.util.List;

public interface AccountService {

    /**
     * Retrieves all accounts.
     *
     * @return list of account responses
     */
    List<AccountResponse> getAll();
}