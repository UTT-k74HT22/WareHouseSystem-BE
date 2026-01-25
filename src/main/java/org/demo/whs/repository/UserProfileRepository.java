package org.demo.whs.repository;

import org.demo.whs.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    /**
     * Find UserProfile by username.
     * @param username username
     * @return Optional of UserProfile
     */
    Optional<UserProfile> findByUsername(String username);
}
