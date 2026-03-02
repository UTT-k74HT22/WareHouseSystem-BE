package org.demo.whs.repository;

import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.repository.custom.UserProfileRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository Interface for managing UserProfile.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long>, UserProfileRepositoryCustom {
    @Query("""
    select u from UserProfile u
    where u.email = :email
    """)
    Optional<UserProfile> findByEmail(String email);

    /**
     * Kiá»ƒm tra email Ä‘Ã£ tá»“n táº¡i hay chÆ°a.
     *
     * @param email email ngÆ°á»i dÃ¹ng
     * @return true náº¿u Ä‘Ã£ tá»“n táº¡i
     */
    boolean existsByEmail(String email);

    /**
     * Find a user profile by its linked account ID.
     *
     * @param accountId account identifier
     * @return optional user profile
     */
    Optional<UserProfile> findByAccountId(String accountId);

    /**
     * Find user profiles by account IDs.
     *
     * @param accountIds account identifiers
     * @return list of user profiles
     */
    List<UserProfile> findByAccountIdIn(Collection<String> accountIds);

}
