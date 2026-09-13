package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRegistrationServiceImpl Unit Tests")
class AccountRegistrationServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountHasRoleRepository accountHasRoleRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountRegistrationServiceImpl accountRegistrationService;

    @Nested
    @DisplayName("register tests")
    class RegisterTests {

        @Test
        @DisplayName("should_RegisterSuccessfully_When_ValidRequest")
        void should_RegisterSuccessfully_When_ValidRequest() {
            // Arrange
            String username = "john.doe";
            String rawPassword = "secret123";
            String roleStr = "WAREHOUSE_STAFF";
            String firstName = "John";
            String lastName = "Doe";
            String email = "john.doe@example.com";
            String phone = "0912345678";

            Role role = Role.builder().name(roleStr).build();
            role.setId("role-1");

            Account savedAccount = Account.builder()
                    .username(username)
                    .password("hashed_secret")
                    .status(AccountStatus.INACTIVE)
                    .build();
            savedAccount.setId("acc-1");

            UserProfile savedProfile = UserProfile.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber(phone)
                    .accountId("acc-1")
                    .build();

            when(accountRepository.existsByUsername(username)).thenReturn(false);
            when(roleRepository.findByName(roleStr)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(rawPassword)).thenReturn("hashed_secret");
            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
            when(accountHasRoleRepository.save(any(AccountHasRole.class))).thenAnswer(i -> i.getArgument(0));
            when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);

            // Act
            var result = accountRegistrationService.register(username, rawPassword, roleStr, firstName, lastName, email, phone);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.account()).isNotNull();
            assertThat(result.userProfile()).isNotNull();
            verify(passwordEncoder).encode(rawPassword);
            verify(accountRepository).save(any(Account.class));
            verify(accountHasRoleRepository).save(any(AccountHasRole.class));
            verify(userProfileRepository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_UsernameAlreadyExists")
        void should_ThrowBadRequestException_When_UsernameAlreadyExists() {
            // Arrange
            when(accountRepository.existsByUsername("existing.user")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> accountRegistrationService.register(
                    "existing.user", "pass", "WAREHOUSE_STAFF",
                    "John", "Doe", "email@test.com", "0900000000"
            ))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.COM_005.getCode()));

            verify(accountRepository, never()).save(any());
            verify(userProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ThrowNotFoundException_When_RoleNotFound")
        void should_ThrowNotFoundException_When_RoleNotFound() {
            // Arrange
            when(accountRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("NONEXISTENT_ROLE")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountRegistrationService.register(
                    "newuser", "pass", "NONEXISTENT_ROLE",
                    "John", "Doe", "email@test.com", "0900000000"
            ))
                    .isInstanceOf(NotFoundException.class)
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.ROLE_001.getCode()));

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_HashPassword_When_Registering")
        void should_HashPassword_When_Registering() {
            // Arrange
            String raw = "plaintext";
            String hashed = "$2a$10$hashedvalue";
            Role role = Role.builder().name("ADMIN").build();
            role.setId("r-1");

            when(accountRepository.existsByUsername(anyString())).thenReturn(false);
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(raw)).thenReturn(hashed);
            when(accountRepository.save(any())).thenAnswer(invocation -> {
                Account a = invocation.getArgument(0);
                a.setId("acc-new");
                return a;
            });
            when(accountHasRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            accountRegistrationService.register("new.user", raw, "ADMIN", "A", "B", "a@b.com", "01");

            // Assert
            verify(passwordEncoder).encode(raw);
            verify(accountRepository).save(argThat(a -> hashed.equals(a.getPassword())));
        }
    }
}
