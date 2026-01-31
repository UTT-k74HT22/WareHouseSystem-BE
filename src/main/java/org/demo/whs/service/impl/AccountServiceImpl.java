package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AccountServiceImpl implements AccountService{

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public PageResponse<AccountResponse> getList(int page, int size) {
        Page<Account> accountPage = accountRepository.findAll(
                PageRequest.of(page, size)
        );

        return PageResponse.from(
                accountPage.map(accountMapper::toResponse)
        );
    }
}
