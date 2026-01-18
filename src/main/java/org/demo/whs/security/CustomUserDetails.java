package org.demo.whs.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.enums.AccountStatus;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Account account;
    private final List<String> roles; // ["ADMIN", "USER"]

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null) return List.of();
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return account.getStatus() == AccountStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return account.getStatus() != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isAccountNonExpired() {
        return account.getStatus() != AccountStatus.DELETED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
