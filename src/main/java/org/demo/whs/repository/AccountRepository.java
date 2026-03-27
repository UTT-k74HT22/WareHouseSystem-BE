package org.demo.whs.repository;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /** Find an account by its username.
     *
     * @param username the username of the account
     * @return an Optional containing the found account or empty if not found
     */
    Optional<Account> findByUsername(String username);

    /**
     * Kiểm tra username đã tồn tại hay chưa.
     *
     * @param username tên đăng nhập
     * @return true nếu đã tồn tại
     */
    boolean existsByUsername(String username);

    /**
     * Lấy users theo roleId, chỉ select những field cần thiết
     * Chúng ta vẫn trả Account entity để mapper xử lý các field null
     */
    @Query("""
        SELECT a
        FROM Account a
        JOIN AccountHasRole ar ON a.id = ar.id.accountId
        WHERE ar.id.roleId = :roleId
    """)
    Page<Account> findUsersByRoleId(String roleId, Pageable pageable);
}
