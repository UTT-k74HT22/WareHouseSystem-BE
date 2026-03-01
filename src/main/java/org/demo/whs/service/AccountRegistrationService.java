package org.demo.whs.service;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.enums.RoleType;

/**
 * Handles the shared provisioning of a system account + user profile.
 * <p>
 * This is the common "onboarding" flow shared between Employee creation
 * and Customer portal-user creation:
 * <ol>
 *   <li>Validate username uniqueness.</li>
 *   <li>Create {@link Account} with hashed password.</li>
 *   <li>Assign the given {@link org.demo.whs.entity.Role} via {@code account_roles}.</li>
 *   <li>Create {@link UserProfile} linked to the new account.</li>
 * </ol>
 * </p>
 */
public interface AccountRegistrationService {

    /**
     * Provision a new account + role assignment + user profile as a single atomic unit.
     *
     * @param username    desired login username (must be unique)
     * @param rawPassword plain-text password (will be hashed internally)
     * @param role        the role to assign to the new account
     * @param firstName   user's first name
     * @param lastName    user's last name
     * @param email       user's email address
     * @param phoneNumber user's phone number (nullable)
     * @return the persisted {@link RegisterAccount} containing the new account and user profile
     */
    RegisterAccount register(
            String username,
            String rawPassword,
            RoleType role,
            String firstName,
            String lastName,
            String email,
            String phoneNumber
    );

    /**
     * Value object returned by {@link #register} containing both
     * the created {@link Account} and {@link UserProfile}.
     *
     * @param account     the persisted account entity
     * @param userProfile the persisted user profile entity
     */
    record RegisterAccount(Account account, UserProfile userProfile) {}
}
