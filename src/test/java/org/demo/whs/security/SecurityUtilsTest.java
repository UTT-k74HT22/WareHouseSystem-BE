package org.demo.whs.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsername_returnsNullWhenNotAuthenticated() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", "pass");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertNull(SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentUsername_returnsUsernameFromUserDetails() {
        UserDetails userDetails = new User("demo", "pass", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, "pass", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("demo", SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentUsername_returnsPrincipalString() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("demo", "pass", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals("demo", SecurityUtils.getCurrentUsername());
    }
}
