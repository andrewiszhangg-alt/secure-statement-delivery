package com.chitsa.account.controller;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.common.exception.EntityNotFoundException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.ValidationUtil;
import com.chitsa.userprofile.service.CurrentProfileProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Accounts page: list and create (customers cannot edit or delete). The account list itself comes from {@link AccountWebModelAdvice}. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountWebController {

    private final AccountService accountService;
    private final CurrentProfileProvider currentProfileProvider;

    @GetMapping("/")
    public String home() {
        return AccountConstant.ACCOUNTS_REDIRECT;
    }

    @GetMapping(AccountConstant.ACCOUNTS_PATH)
    public String list() {
        return AccountConstant.ACCOUNTS_VIEW;
    }

    @PostMapping(AccountConstant.ACCOUNTS_PATH)
    public String create(
            @RequestParam String accountTypeId,
            @RequestParam(defaultValue = AccountConstant.ACCOUNTS_VIEW) String from,
            RedirectAttributes redirectAttributes) {
        try {
            var created = accountService.create(toRequest(accountTypeId));
            redirectAttributes.addFlashAttribute(AccountConstant.MESSAGE, AccountConstant.ACCOUNT_CREATED);
            if (AccountConstant.FROM_STATEMENTS.equals(from)) {
                return AccountConstant.ACCOUNTS_REDIRECT + "/" + created.id() + "/" + AccountConstant.FROM_STATEMENTS;
            }
        } catch (InvalidParameterException | EntityNotFoundException | DataIntegrityViolationException ex) {
            log.warn("Account create failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute(AccountConstant.ERROR, errorMessage(ex));
        }
        return AccountConstant.ACCOUNTS_REDIRECT;
    }

    private AccountRequestDto toRequest(String accountTypeId) {
        return new AccountRequestDto(currentProfileProvider.profileId(), parseTypeId(accountTypeId));
    }

    private static UUID parseTypeId(String value) {
        return ValidationUtil.requireUuid(value, AccountConstant.INVALID_ACCOUNT_TYPE);
    }

    private static String errorMessage(RuntimeException ex) {
        return ex instanceof DataIntegrityViolationException ? AccountConstant.ACCOUNT_CONFLICT : ex.getMessage();
    }
}
