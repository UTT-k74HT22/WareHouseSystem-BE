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
}