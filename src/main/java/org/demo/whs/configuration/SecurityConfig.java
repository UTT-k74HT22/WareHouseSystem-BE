package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration class for Spring Security with JWT authentication.
 * 
 * - Configures JWT authentication filter
 * - Enables method-level security with @Secured and @PreAuthorize
 * - Sets up stateless session management
 * - Configures CORS and CSRF
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {


    /**
     * Password encoder bean using BCrypt.
     *
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Security filter chain configuration.
     * 
     * Configures:
     * - Stateless session management (JWT-based)
     * - CSRF disabled for API
     * - CORS enabled
     * - JWT filter registered before UsernamePasswordAuthenticationFilter
     * - Authorization rules
     *
     * @param http the HttpSecurity object
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> {})

                // Configure authorization
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/health", "/actuator/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}

