package org.demo.whs.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.enums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Account account;
    private final List<String> roles; // ["ADMIN", "USER"]
    private final Set<String> permissions; // ["PERM_PRODUCT_READ", ...]

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>();

        Stream<String> roleStream = roles == null ? Stream.empty() : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role);

        Stream<String> permissionStream = permissions == null ? Stream.empty() : permissions.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(permission -> !permission.isBlank());

        Stream.concat(roleStream, permissionStream)
                .forEach(authorities::add);

        return authorities.stream()
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
