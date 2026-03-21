package org.demo.whs.repository;

import org.demo.whs.entity.Role;
import org.demo.whs.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    @Query(value = """
    SELECT r.name
    FROM roles r
    INNER JOIN account_roles ar ON r.id = ar.role_id
    INNER JOIN accounts a ON ar.account_id = a.id
    WHERE a.username = :username
    """, nativeQuery = true)
    List<String> findRoleNamesByUsername(@Param("username") String username);

    @Query(value = """
    SELECT r.name
    FROM roles r
    INNER JOIN account_roles ar ON r.id = ar.role_id
    WHERE ar.account_id = :accountId
    """, nativeQuery = true)
    List<String> findRoleNamesByAccountId(@Param("accountId") String accountId);

    Optional<Role> findByName(RoleType name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByIsDefaultTrue();

    boolean existsByCode(String code);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Role r SET r.isDefault = false WHERE r.isDefault = true")
    void updateAllIsDefaultToFalse();
}