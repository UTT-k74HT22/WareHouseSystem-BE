package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
    /**
     * Convert RegisterRequest to UserProfile
     * */
    public UserProfile toUserProfile(RegisterRequest request, Account account) {
        return UserProfile.builder()
                .accountId(account.getId())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();
    }

    /**
     * Create a UserProfile entity with the given details and associated Account.
     *
     * @param firstName   the first name of the user
     * @param lastName    the last name of the user
     * @param email       the email address of the user
     * @param phoneNumber the phone number of the user
     * @param account     the associated Account entity
     * @return a new UserProfile entity
     */
    public static UserProfile getUserProfile(String firstName, String lastName, String email, String phoneNumber, Account account) {
        return UserProfile.builder()
                .accountId(account.getId())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();
    }

}