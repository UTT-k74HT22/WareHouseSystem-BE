package org.demo.whs.mapper;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.AccountRoleId;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper class for UserRole transformations.
 */
@Component
public class UserRoleMapper {

    public AccountHasRole createEntity(String userId, String roleId) {

        if (userId == null || roleId == null) {
            return null;
        }

        AccountRoleId id = new AccountRoleId(userId, roleId);

        AccountHasRole entity = new AccountHasRole();
        entity.setId(id);

        return entity;
    }
}