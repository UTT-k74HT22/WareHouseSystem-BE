package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    /**
     * Maps Account entity to AccountResponse DTO.
     *
     * @param account the account entity
     * @return account response DTO
     */
    public AccountResponse toResponse(Account account){
        if (account == null){
            return null;
        }
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .createdBy(account.getCreatedBy())
                .updatedBy(account.getUpdatedBy())
                .build();
    }
}
