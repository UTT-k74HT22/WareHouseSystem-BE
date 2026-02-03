package org.demo.whs.repository;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    /** Find a user profile by its email.
     *
     * @param email the email of the user profile
     * @return an Optional containing the found user profile or empty if not found
     */
    Optional<UserProfile> findByEmail(String email);
}
