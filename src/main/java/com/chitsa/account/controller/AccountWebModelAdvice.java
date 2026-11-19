package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.accountstatement.controller.AccountStatementWebController;
import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Comparator;
import java.util.List;

/** Puts the profile's accounts and the account types on every page (header dropdown, dialog). */
@ControllerAdvice(assignableTypes = {AccountWebController.class, AccountStatementWebController.class})
@RequiredArgsConstructor
public class AccountWebModelAdvice {

    private final AccountService accountService;
    private final AccountTypeService accountTypeService;
    private final CurrentProfileProvider currentProfileProvider;

    @ModelAttribute("accounts")
    public List<AccountResponseDto> accounts() {
        return accountService.findByUserProfileId(currentProfileProvider.profileId()).stream()
                .sorted(Comparator.comparing((AccountResponseDto a) -> a.accountType().label())
                        .thenComparing(AccountResponseDto::createTime))
                .toList();
    }

    @ModelAttribute("accountTypes")
    public List<AccountTypeResponseDto> accountTypes() {
        return accountTypeService.findAll();
    }
}
