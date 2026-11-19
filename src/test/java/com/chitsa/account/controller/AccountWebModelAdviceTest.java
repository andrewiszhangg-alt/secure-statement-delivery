package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWebModelAdviceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountTypeService accountTypeService;

    @Mock
    private CurrentProfileProvider currentProfileProvider;

    @InjectMocks
    private AccountWebModelAdvice advice;

    @Test
    void accountsAreSortedByTypeThenTime() {
        var profileId = UUID.randomUUID();
        when(currentProfileProvider.profileId()).thenReturn(profileId);
        var laterSavings = account("Savings", Instant.parse("2026-02-01T00:00:00Z"));
        var business = account("Business", Instant.parse("2026-03-01T00:00:00Z"));
        var earlierSavings = account("Savings", Instant.parse("2026-01-01T00:00:00Z"));
        when(accountService.findByUserProfileId(profileId)).thenReturn(List.of(laterSavings, business, earlierSavings));

        assertThat(advice.accounts()).containsExactly(business, earlierSavings, laterSavings);
    }

    @Test
    void accountTypesComeFromTheCatalog() {
        var types = List.of(AccountTypeResponseDto.builder().code("SAVINGS").label("Savings").build());
        when(accountTypeService.findAll()).thenReturn(types);

        assertThat(advice.accountTypes()).isEqualTo(types);
    }

    private static AccountResponseDto account(String label, Instant created) {
        return AccountResponseDto.builder()
                .id(UUID.randomUUID())
                .createTime(created)
                .accountType(AccountTypeResponseDto.builder().label(label).build())
                .build();
    }
}
