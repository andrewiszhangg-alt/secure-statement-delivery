package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWebControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    @InjectMocks
    private AccountWebController controller;

    @Test
    void homeRedirectsToAccounts() {
        assertThat(controller.home()).isEqualTo(AccountConstant.ACCOUNTS_REDIRECT);
    }

    @Test
    void listShowsTheAccountsPage() {
        assertThat(controller.list()).isEqualTo(AccountConstant.ACCOUNTS_VIEW);
    }

    @Test
    void createStoresTheAccount() {
        var typeId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var profileId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        when(accountService.create(org.mockito.ArgumentMatchers.any())).thenReturn(response());
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.create(typeId.toString(), "accounts", redirect)).isEqualTo(AccountConstant.ACCOUNTS_REDIRECT);
        assertThat(redirect.getFlashAttributes().get(AccountConstant.MESSAGE)).isEqualTo(AccountConstant.ACCOUNT_CREATED);
    }

    @Test
    void createFromStatementsOpensTheNewAccount() {
        var created = response();
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());
        when(accountService.create(org.mockito.ArgumentMatchers.any())).thenReturn(created);

        var view = controller.create(UUID.randomUUID().toString(), AccountConstant.FROM_STATEMENTS, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo(AccountConstant.ACCOUNTS_REDIRECT + "/" + created.id() + "/" + AccountConstant.FROM_STATEMENTS);
    }

    @Test
    void createRejectsAnInvalidTypeId() {
        var redirect = new RedirectAttributesModelMap();

        assertThat(controller.create("not-a-uuid", "accounts", redirect)).isEqualTo(AccountConstant.ACCOUNTS_REDIRECT);
        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR)).isEqualTo(AccountConstant.INVALID_ACCOUNT_TYPE);
    }

    @Test
    void createReportsAConflict() {
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());
        when(accountService.create(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        var redirect = new RedirectAttributesModelMap();

        controller.create(UUID.randomUUID().toString(), "accounts", redirect);

        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR)).isEqualTo(AccountConstant.ACCOUNT_CONFLICT);
    }

    @Test
    void createShowsTheServiceMessage() {
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());
        when(accountService.create(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidParameterException("incomplete"));
        var redirect = new RedirectAttributesModelMap();

        controller.create(UUID.randomUUID().toString(), "accounts", redirect);

        assertThat(redirect.getFlashAttributes().get(AccountConstant.ERROR)).isEqualTo("incomplete");
    }

    private static AccountResponseDto response() {
        return AccountResponseDto.builder()
                .id(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"))
                .userProfileId(UUID.randomUUID())
                .build();
    }
}
