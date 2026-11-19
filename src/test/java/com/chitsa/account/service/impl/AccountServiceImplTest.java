package com.chitsa.account.service.impl;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.entity.Account;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.mapper.AccountMapper;
import com.chitsa.account.repository.AccountRepository;
import com.chitsa.account.service.AccountAccessGuard;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.account.validator.AccountValidator;
import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.validator.RequestBeanValidator;
import com.chitsa.security.service.CurrentUser;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import com.chitsa.userprofile.service.UserProfileService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AccountTypeService accountTypeService;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    private AccountServiceImpl service;

    private final UUID owner = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID typeId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        var validator = new AccountValidator(
                new RequestBeanValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        var guard = new AccountAccessGuard(currentUser, currentProfileProvider);
        service = new AccountServiceImpl(repository, new AccountMapper(), validator, guard, userProfileService, accountTypeService);
    }

    @Test
    void createStoresAnAccountForACompleteProfile() {
        asOwner();
        when(repository.save(any())).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
            return account;
        });
        when(accountTypeService.findById(typeId)).thenReturn(type(typeId, "SAVINGS", "Savings"));

        var response = service.create(new AccountRequestDto(owner, typeId));

        verify(userProfileService).requireCompleteProfile(owner);
        verify(accountTypeService).requireExists(typeId);
        assertThat(response.id()).isEqualTo(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        assertThat(response.userProfileId()).isEqualTo(owner);
        assertThat(response.accountType().code()).isEqualTo("SAVINGS");
    }

    @Test
    void createRejectsAForeignProfile() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.idpUuid()).thenReturn(UUID.randomUUID());
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.create(new AccountRequestDto(owner, typeId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(AccountConstant.PROFILE_ACCESS_DENIED);
        verify(repository, never()).save(any());
    }

    @Test
    void createStopsWhenTheProfileIsIncomplete() {
        asOwner();
        org.mockito.Mockito.doThrow(new InvalidParameterException("incomplete"))
                .when(userProfileService).requireCompleteProfile(owner);

        assertThatThrownBy(() -> service.create(new AccountRequestDto(owner, typeId)))
                .isInstanceOf(InvalidParameterException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesTheTypeAndLeavesTheOwner() {
        var id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        var nextType = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        var entity = account(id, owner, typeId);
        asOwner();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(accountTypeService.findById(nextType)).thenReturn(type(nextType, "CREDIT_CARD", "Credit card"));

        var response = service.update(id, new AccountRequestDto(UUID.randomUUID(), nextType));

        assertThat(entity.getUserProfileId()).isEqualTo(owner);
        assertThat(entity.getAccountTypeId()).isEqualTo(nextType);
        assertThat(response.accountType().code()).isEqualTo("CREDIT_CARD");
    }

    @Test
    void findByIdReturnsAnOwnedAccount() {
        var id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        asOwner();
        when(repository.findById(id)).thenReturn(Optional.of(account(id, owner, typeId)));
        when(accountTypeService.findById(typeId)).thenReturn(type(typeId, "SAVINGS", "Savings"));

        var response = service.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.accountType().label()).isEqualTo("Savings");
    }

    @Test
    void findByIdThrowsWhenTheAccountIsMissing() {
        var id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(AccountConstant.NOT_FOUND + id);
    }

    @Test
    void findByIdHidesAForeignAccount() {
        var id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(repository.findById(id)).thenReturn(Optional.of(account(id, owner, typeId)));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.idpUuid()).thenReturn(UUID.randomUUID());
        when(currentProfileProvider.profileId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(AccountConstant.NOT_FOUND + id);
    }

    @Test
    void listLoadsTypesOnce() {
        var otherType = UUID.fromString("22222222-2222-2222-2222-222222222222");
        asOwner();
        when(accountTypeService.findAll()).thenReturn(List.of(
                type(typeId, "SAVINGS", "Savings"),
                type(otherType, "BUSINESS", "Business")));
        when(repository.findByUserProfileId(owner)).thenReturn(List.of(
                account(UUID.randomUUID(), owner, typeId),
                account(UUID.randomUUID(), owner, otherType)));

        var accounts = service.findByUserProfileId(owner);

        assertThat(accounts).extracting(account -> account.accountType().code()).containsExactly("SAVINGS", "BUSINESS");
        verify(accountTypeService).findAll();
        verify(accountTypeService, never()).findById(any());
    }

    @Test
    void deleteRemovesAnOwnedAccount() {
        var id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var entity = account(id, owner, typeId);
        asOwner();
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.deleteById(id);

        verify(repository).delete(entity);
        verify(repository).flush();
    }

    private void asOwner() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentProfileProvider.profileId()).thenReturn(owner);
    }

    private static Account account(UUID id, UUID owner, UUID typeId) {
        var account = new Account();
        account.setId(id);
        account.setUserProfileId(owner);
        account.setAccountTypeId(typeId);
        return account;
    }

    private static AccountTypeResponseDto type(UUID id, String code, String label) {
        return AccountTypeResponseDto.builder().id(id).code(code).label(label).build();
    }
}
