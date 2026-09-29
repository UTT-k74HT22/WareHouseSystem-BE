package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.request.User.ResetUserPasswordRequest;
import org.demo.whs.entity.dto.request.User.UpdateUserRequest;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("getAllUserWithRoleManager tests")
    class GetAllUserWithRoleManagerTests {

        @Test
        @DisplayName("should_ReturnCombinedAndDedupedList_When_AdminAndManagerExist")
        void should_ReturnCombinedAndDedupedList_When_AdminAndManagerExist() {
            // Arrange
            AccountResponse admin1 = AccountResponse.builder().accountId("acc-1").username("admin1").build();
            AccountResponse mgr1 = AccountResponse.builder().accountId("acc-2").username("mgr1").build();
            // acc-1 appears in both ADMIN and MANAGER lists -> should be deduplicated
            AccountResponse adminAlso = AccountResponse.builder().accountId("acc-1").username("admin1").build();

            when(userProfileRepository.getAccountsByRole("ADMIN")).thenReturn(List.of(admin1));
            when(userProfileRepository.getAccountsByRole("MANAGER")).thenReturn(List.of(mgr1, adminAlso));

            // Act
            List<AccountResponse> result = userService.getAllUserWithRoleManager();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AccountResponse::getAccountId)
                    .containsExactlyInAnyOrder("acc-1", "acc-2");
            verify(userProfileRepository).getAccountsByRole("ADMIN");
            verify(userProfileRepository).getAccountsByRole("MANAGER");
        }

        @Test
        @DisplayName("should_ReturnEmptyList_When_NoAdminOrManagerExists")
        void should_ReturnEmptyList_When_NoAdminOrManagerExists() {
            when(userProfileRepository.getAccountsByRole("ADMIN")).thenReturn(List.of());
            when(userProfileRepository.getAccountsByRole("MANAGER")).thenReturn(List.of());

            List<AccountResponse> result = userService.getAllUserWithRoleManager();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_ReturnOnlyAdmins_When_NoManagerExists")
        void should_ReturnOnlyAdmins_When_NoManagerExists() {
            AccountResponse admin = AccountResponse.builder().accountId("acc-1").username("admin").build();
            when(userProfileRepository.getAccountsByRole("ADMIN")).thenReturn(List.of(admin));
            when(userProfileRepository.getAccountsByRole("MANAGER")).thenReturn(List.of());

            List<AccountResponse> result = userService.getAllUserWithRoleManager();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountId()).isEqualTo("acc-1");
        }
    }

    @Nested
    @DisplayName("getUserById tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("should_ReturnAccountResponse_When_AccountIdExists")
        void should_ReturnAccountResponse_When_AccountIdExists() {
            Account account = Account.builder()
                    .username("user.ten")
                    .password("hashed")
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setId("acc-10");
            AccountResponse expected = AccountResponse.builder()
                    .accountId("acc-10")
                    .username("user.ten")
                    .build();

            when(accountRepository.findById("acc-10")).thenReturn(Optional.of(account));
            when(userProfileRepository.findByAccountId("acc-10")).thenReturn(Optional.empty());
            when(accountMapper.toAccountResponseWithProfile(account, null)).thenReturn(expected);

            AccountResponse actual = userService.getUserById("acc-10");

            assertThat(actual).isNotNull();
            assertThat(actual.getAccountId()).isEqualTo("acc-10");
            assertThat(actual.getUsername()).isEqualTo("user.ten");
            verify(accountRepository).findById("acc-10");
        }

        @Test
        @DisplayName("should_ThrowNotFound_When_AccountIdNotFound")
        void should_ThrowNotFound_When_AccountIdNotFound() {
            when(accountRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById("nonexistent"))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ROLE_001.getCode());
        }
    }

    @Nested
    @DisplayName("update tests")
    class UpdateTests {

        private Account account(String id, String username) {
            Account account = Account.builder()
                    .username(username)
                    .password("hashed")
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setId(id);
            return account;
        }

        private UserProfile profile(String accountId, String email) {
            UserProfile profile = UserProfile.builder()
                    .accountId(accountId)
                    .firstName("Ten")
                    .lastName("User")
                    .email(email)
                    .build();
            profile.setId("profile-1");
            return profile;
        }

        @Test
        @DisplayName("should_UpdateProfileFields_When_RequestIsValid")
        void should_UpdateProfileFields_When_RequestIsValid() {
            Account account = account("acc-1", "user.ten");
            UserProfile profile = profile("acc-1", "old@example.com");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(profile));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(accountMapper.toAccountResponseWithProfile(any(Account.class), any()))
                    .thenReturn(AccountResponse.builder().accountId("acc-1").build());

            AccountResponse result = userService.update(
                    "acc-1",
                    UpdateUserRequest.builder().email("new@example.com").firstName("Moi").build());

            assertThat(result.getAccountId()).isEqualTo("acc-1");
            assertThat(profile.getEmail()).isEqualTo("new@example.com");
            assertThat(profile.getFirstName()).isEqualTo("Moi");
            assertThat(account.getUsername()).isEqualTo("user.ten");
        }

        @Test
        @DisplayName("should_ThrowBadRequest_When_UsernameAlreadyExists")
        void should_ThrowBadRequest_When_UsernameAlreadyExists() {
            Account account = account("acc-1", "user.ten");
            Account other = account("acc-2", "user.khac");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(accountRepository.findByUsername("user.khac")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> userService.update(
                    "acc-1",
                    UpdateUserRequest.builder().username("user.khac").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_005.getCode());

            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequest_When_EmailAlreadyExists")
        void should_ThrowBadRequest_When_EmailAlreadyExists() {
            Account account = account("acc-1", "user.ten");
            UserProfile profile = profile("acc-1", "old@example.com");
            UserProfile other = profile("acc-2", "taken@example.com");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(profile));
            when(userProfileRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> userService.update(
                    "acc-1",
                    UpdateUserRequest.builder().email("taken@example.com").build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_005.getCode());

            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("should_ThrowNotFound_When_AccountDoesNotExist")
        void should_ThrowNotFound_When_AccountDoesNotExist() {
            when(accountRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(
                    "missing",
                    UpdateUserRequest.builder().email("new@example.com").build()))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ROLE_001.getCode());
        }

        @Test
        @DisplayName("should_CreateProfile_When_MissingAndFullFieldsProvided")
        void should_CreateProfile_When_MissingAndFullFieldsProvided() {
            Account account = account("acc-1", "user.ten");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());
            when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(accountMapper.toAccountResponseWithProfile(any(Account.class), any()))
                    .thenReturn(AccountResponse.builder().accountId("acc-1").build());

            AccountResponse result = userService.update(
                    "acc-1",
                    UpdateUserRequest.builder()
                            .email("new@example.com")
                            .firstName("Moi")
                            .lastName("Ten")
                            .build());

            assertThat(result.getAccountId()).isEqualTo("acc-1");
            verify(userProfileRepository).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should_UpdateStatus_When_OnlyStatusProvided")
        void should_UpdateStatus_When_OnlyStatusProvided() {
            Account account = Account.builder()
                    .username("user.ten")
                    .password("hashed")
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setId("acc-1");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());
            when(accountMapper.toAccountResponseWithProfile(any(Account.class), any()))
                    .thenAnswer(invocation -> AccountResponse.builder()
                            .accountId("acc-1")
                            .status(((Account) invocation.getArgument(0)).getStatus())
                            .build());

            AccountResponse result = userService.update(
                    "acc-1",
                    UpdateUserRequest.builder().status(AccountStatus.SUSPENDED).build());

            assertThat(result.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
            verify(accountRepository).save(any(Account.class));
            verify(userProfileRepository, never()).save(any(UserProfile.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequest_When_StatusIsDeleted")
        void should_ThrowBadRequest_When_StatusIsDeleted() {
            assertThatThrownBy(() -> userService.update(
                    "acc-1",
                    UpdateUserRequest.builder().status(AccountStatus.DELETED).build()))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("resetPassword tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("should_EncodeAndSavePassword_When_RequestIsValid")
        void should_EncodeAndSavePassword_When_RequestIsValid() {
            Account account = Account.builder()
                    .username("user.ten")
                    .password("old-hashed")
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setId("acc-1");

            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hashed");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());
            when(accountMapper.toAccountResponseWithProfile(any(Account.class), any()))
                    .thenReturn(AccountResponse.builder().accountId("acc-1").build());

            AccountResponse result = userService.resetPassword(
                    "acc-1",
                    ResetUserPasswordRequest.builder().newPassword("NewPass1!").build());

            assertThat(account.getPassword()).isEqualTo("new-hashed");
            assertThat(result.getAccountId()).isEqualTo("acc-1");
            verify(passwordEncoder).encode("NewPass1!");
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should_ThrowNotFound_When_AccountDoesNotExist")
        void should_ThrowNotFound_When_AccountDoesNotExist() {
            when(accountRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.resetPassword(
                    "missing",
                    ResetUserPasswordRequest.builder().newPassword("NewPass1!").build()))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ROLE_001.getCode());

            verify(passwordEncoder, never()).encode(any());
        }
    }
}
