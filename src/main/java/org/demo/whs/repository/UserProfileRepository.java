package org.demo.whs.repository;

import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.repository.custom.UserProfileRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository Interface for managing UserProfile.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String>, UserProfileRepositoryCustom {
}
