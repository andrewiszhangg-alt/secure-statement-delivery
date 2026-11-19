package com.chitsa.admin;

import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.exception.EntityNotFoundException;
import com.chitsa.common.exception.InvalidParameterException;
import com.chitsa.common.util.ValidationUtil;
import com.chitsa.mediaresource.config.MediaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Admin forms for a new account type and a statement upload. The role check lives on the security chain. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminWebController {

    static final String PATH = "/admin";
    static final String VIEW = "admin";
    static final String REDIRECT = "redirect:/admin";
    static final String TYPE_SAVED = "Account type saved";
    static final String STATEMENT_SAVED = "Statement saved";

    private final AccountTypeService accountTypeService;
    private final AccountService accountService;
    private final AccountStatementService accountStatementService;
    private final MediaProperties mediaProperties;

    @GetMapping(PATH)
    public String page(Model model) {
        model.addAttribute("accountTypes", accountTypeService.findAll());
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("accept", mediaProperties.acceptAttribute());
        return VIEW;
    }

    @PostMapping("/admin/account-types")
    public String createType(
            @RequestParam String code,
            @RequestParam String label,
            RedirectAttributes redirectAttributes) {
        try {
            accountTypeService.create(code, label);
            return flash(redirectAttributes, AccountConstant.MESSAGE, TYPE_SAVED);
        } catch (InvalidParameterException | DuplicateResourceException ex) {
            log.warn("Account type create failed: {}", ex.getMessage());
            return flash(redirectAttributes, AccountConstant.ERROR, ex.getMessage());
        }
    }

    @PostMapping("/admin/statements")
    public String createStatement(
            @RequestParam String accountId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            accountStatementService.create(new AccountStatementRequestDto(parseAccountId(accountId), file));
            return flash(redirectAttributes, AccountConstant.MESSAGE, STATEMENT_SAVED);
        } catch (InvalidParameterException | EntityNotFoundException ex) {
            log.warn("Statement upload failed: {}", ex.getMessage());
            return flash(redirectAttributes, AccountConstant.ERROR, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Statement upload conflict: {}", ex.getMessage());
            return flash(redirectAttributes, AccountConstant.ERROR, AccountConstant.ACCOUNT_CONFLICT);
        }
    }

    private static String flash(RedirectAttributes redirectAttributes, String key, String value) {
        redirectAttributes.addFlashAttribute(key, value);
        return REDIRECT;
    }

    private static UUID parseAccountId(String value) {
        return ValidationUtil.requireUuid(value, AccountConstant.NOT_FOUND + value);
    }
}
