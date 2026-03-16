package org.demo.whs.repository;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    /**
     * Checks whether a permission already exists by its unique name.
     *
     * @param Name permission name
     * @return true if permission exists, false otherwise
     */
    boolean existsByName(String Name);

    boolean existsByResourceAndAction(String resource, ActionType action);
}
