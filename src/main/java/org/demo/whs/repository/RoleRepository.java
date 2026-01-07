package org.demo.whs.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.demo.whs.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    @Query(value = """
    SELECT r.name
    FROM roles r
    INNER JOIN account_roles ar
    ON r.id = ar.role_id
    INNER JOIN accounts a
    ON ar.account_id = a.id
    WHERE a.username = :username
    """, nativeQuery = true)
    List<String> findRoleNamesByUsername(@Param("username") String username);
}
