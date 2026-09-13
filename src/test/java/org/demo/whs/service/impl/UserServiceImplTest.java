package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

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
            AccountResponse expected = AccountResponse.builder()
                    .accountId("acc-10")
                    .username("user.ten")
                    .build();

            when(userProfileRepository.getAccountById("acc-10")).thenReturn(expected);

            AccountResponse actual = userService.getUserById("acc-10");

            assertThat(actual).isNotNull();
            assertThat(actual.getAccountId()).isEqualTo("acc-10");
            assertThat(actual.getUsername()).isEqualTo("user.ten");
            verify(userProfileRepository).getAccountById("acc-10");
        }

        @Test
        @DisplayName("should_ReturnNull_When_AccountIdNotFound")
        void should_ReturnNull_When_AccountIdNotFound() {
            when(userProfileRepository.getAccountById("nonexistent")).thenReturn(null);

            AccountResponse result = userService.getUserById("nonexistent");

            assertThat(result).isNull();
        }
    }
}
