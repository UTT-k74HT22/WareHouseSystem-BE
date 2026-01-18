package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.mapper.AuthMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho AuthServiceImpl
 * Sử dụng Mockito để mock dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("Authenticate Tests")
    class AuthenticateTests {

        @Test
        @DisplayName("Should authenticate successfully with valid credentials")
        void authenticate_Success() {
            // Given
            String username = "testuser";
            String password = "password123";
            String encodedPassword = "$2a$10$encoded";
            LoginRequest request = new LoginRequest(username, password);

            Account account = new Account();
            account.setUsername(username);
            account.setPassword(encodedPassword);
            account.setStatus(AccountStatus.ACTIVE);

            List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
            String accessToken = "access-token-123";
            String refreshToken = "refresh-token-456";
            String accessExpiration = "3600";
            String refreshExpiration = "86400";

            AuthResponse expectedResponse = new AuthResponse(
                    accessToken, refreshToken, accessExpiration, refreshExpiration, null
            );

            // When
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
            when(roleRepository.findRoleNamesByUsername(username)).thenReturn(roles);
            when(jwtProvider.buildAccessToken(account, roles)).thenReturn(accessToken);
            when(jwtProvider.buildRefreshToken(account)).thenReturn(refreshToken);
            when(jwtProvider.getExpirationAccessToken(accessToken)).thenReturn(accessExpiration);
            when(jwtProvider.getExpirationRefreshToken(refreshToken)).thenReturn(refreshExpiration);
            when(authMapper.toResponse(accessToken, accessExpiration, refreshToken, refreshExpiration, null))
                    .thenReturn(expectedResponse);

            AuthResponse result = authService.authenticate(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(accessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshToken);

            verify(accountRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, encodedPassword);
            verify(roleRepository).findRoleNamesByUsername(username);
            verify(jwtProvider).buildAccessToken(account, roles);
            verify(jwtProvider).buildRefreshToken(account);
        }

        @Test
        @DisplayName("Should throw AuthenticationFailedException when user not found")
        void authenticate_UserNotFound() {
            // Given
            String username = "nonexistent";
            LoginRequest request = new LoginRequest(username, "password");

            // When
            when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_001");

            verify(accountRepository).findByUsername(username);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw AuthenticationFailedException when password is incorrect")
        void authenticate_WrongPassword() {
            // Given
            String username = "testuser";
            String password = "wrongpassword";
            String encodedPassword = "$2a$10$encoded";
            LoginRequest request = new LoginRequest(username, password);

            Account account = new Account();
            account.setUsername(username);
            account.setPassword(encodedPassword);
            account.setStatus(AccountStatus.ACTIVE);

            // When
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

            // Then
            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_001");

            verify(accountRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, encodedPassword);
            verify(jwtProvider, never()).buildAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should throw AuthenticationFailedException when account is inactive")
        void authenticate_InactiveAccount() {
            // Given
            String username = "inactiveuser";
            String password = "password123";
            String encodedPassword = "$2a$10$encoded";
            LoginRequest request = new LoginRequest(username, password);

            Account account = new Account();
            account.setUsername(username);
            account.setPassword(encodedPassword);
            account.setStatus(AccountStatus.INACTIVE);

            // When
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

            // Then
            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_004");

            verify(accountRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, encodedPassword);
            verify(roleRepository, never()).findRoleNamesByUsername(anyString());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully with valid refresh token")
        void refreshToken_Success() {
            // Given
            String refreshToken = "valid-refresh-token";
            String username = "testuser";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            Account account = new Account();
            account.setUsername(username);
            account.setStatus(AccountStatus.ACTIVE);

            List<String> roles = Arrays.asList("ROLE_USER");
            String newAccessToken = "new-access-token-123";
            String accessExpiration = "3600";

            RefreshTokenResponse expectedResponse = new RefreshTokenResponse(newAccessToken, accessExpiration);

            // When
            when(jwtProvider.validateToken(refreshToken)).thenReturn(false); // false means valid
            when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(refreshToken)).thenReturn(username);
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(roleRepository.findRoleNamesByUsername(username)).thenReturn(roles);
            when(jwtProvider.buildAccessToken(account, roles)).thenReturn(newAccessToken);
            when(jwtProvider.getExpirationAccessToken(newAccessToken)).thenReturn(accessExpiration);
            when(authMapper.toRefreshResponse(newAccessToken, accessExpiration)).thenReturn(expectedResponse);

            RefreshTokenResponse result = authService.refreshToken(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(result.getExpireAccessToken()).isEqualTo(accessExpiration);

            verify(jwtProvider).validateToken(refreshToken);
            verify(jwtProvider).isRefreshToken(refreshToken);
            verify(jwtProvider).getUsernameFromToken(refreshToken);
            verify(accountRepository).findByUsername(username);
            verify(roleRepository).findRoleNamesByUsername(username);
            verify(jwtProvider).buildAccessToken(account, roles);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when refresh token is invalid")
        void refreshToken_InvalidToken() {
            // Given
            String invalidToken = "invalid-token";
            RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

            // When - validateToken returns true when token is invalid
            when(jwtProvider.validateToken(invalidToken)).thenReturn(true);

            // Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_006");

            verify(jwtProvider).validateToken(invalidToken);
            verify(accountRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when token is not a refresh token")
        void refreshToken_NotRefreshToken() {
            // Given
            String accessToken = "access-token-not-refresh";
            RefreshTokenRequest request = new RefreshTokenRequest(accessToken);

            // When
            when(jwtProvider.validateToken(accessToken)).thenReturn(false);
            when(jwtProvider.isRefreshToken(accessToken)).thenReturn(false);

            // Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_006");

            verify(jwtProvider).validateToken(accessToken);
            verify(jwtProvider).isRefreshToken(accessToken);
            verify(accountRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Should throw AuthenticationFailedException when user not found during refresh")
        void refreshToken_UserNotFound() {
            // Given
            String refreshToken = "valid-token";
            String username = "nonexistent";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            // When
            when(jwtProvider.validateToken(refreshToken)).thenReturn(false);
            when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(refreshToken)).thenReturn(username);
            when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_001");

            verify(jwtProvider).getUsernameFromToken(refreshToken);
            verify(accountRepository).findByUsername(username);
            verify(jwtProvider, never()).buildAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should throw AuthenticationFailedException when account is inactive during refresh")
        void refreshToken_InactiveAccount() {
            // Given
            String refreshToken = "valid-token";
            String username = "inactiveuser";
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            Account account = new Account();
            account.setUsername(username);
            account.setStatus(AccountStatus.INACTIVE);

            // When
            when(jwtProvider.validateToken(refreshToken)).thenReturn(false);
            when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(refreshToken)).thenReturn(username);
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));

            // Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_004");

            verify(jwtProvider).getUsernameFromToken(refreshToken);
            verify(accountRepository).findByUsername(username);
            verify(roleRepository, never()).findRoleNamesByUsername(anyString());
        }
    }
}
