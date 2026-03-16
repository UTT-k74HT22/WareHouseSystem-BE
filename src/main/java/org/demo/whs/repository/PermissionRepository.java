package org.demo.whs.repository;

import org.demo.whs.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    /**
     * Checks whether a permission already exists by its unique Name.
     *
     * @param Name permission Name
     * @return true if permission exists, false otherwise
     */
    boolean existsByName(String Name);

}
