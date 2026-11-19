package com.chitsa.account.service;

import com.chitsa.account.entity.Account;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAccessGuardTest {

    @Mock
    private CurrentUser currentUser;

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    private AccountAccessGuard guard;

    private final UUID owner = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        guard = new AccountAccessGuard(currentUser, currentProfileProvider);
    }

    @Test
    void ownerCanOpenTheAccount() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentProfileProvider.profileId()).thenReturn(owner);

        guard.assertCanAccess(account(owner));
    }

    @Test
    void adminCanOpenAnyAccount() {
        when(currentUser.isAdmin()).thenReturn(true);

        guard.assertCanAccess(account(UUID.randomUUID()));
    }

    @Test
    void foreignAccountIsNotFound() {
        var account = account(owner);
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.idpUuid()).thenReturn(UUID.randomUUID());
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> guard.assertCanAccess(account))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(AccountConstant.NOT_FOUND + account.getId());
    }

    @Test
    void foreignProfileIsForbidden() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.idpUuid()).thenReturn(UUID.randomUUID());
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> guard.assertCanAccessProfile(owner))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccountConstant.PROFILE_ACCESS_DENIED);
    }

    @Test
    void aCustomerIsNotAnAdmin() {
        when(currentUser.isAdmin()).thenReturn(false);

        assertThatThrownBy(guard::assertAdmin)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccountConstant.PROFILE_ACCESS_DENIED);
    }

    @Test
    void anAdminPasses() {
        when(currentUser.isAdmin()).thenReturn(true);

        guard.assertAdmin();
    }

    private static Account account(UUID owner) {
        var account = new Account();
        account.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        account.setUserProfileId(owner);
        return account;
    }
}
