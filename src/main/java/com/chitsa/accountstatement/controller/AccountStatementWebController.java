package com.chitsa.accountstatement.controller;

import com.chitsa.account.service.AccountService;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/** Statements page: paged list; View redirects to a fresh presigned link and Copy fetches one as JSON. */
@Controller
@RequestMapping(AccountStatementConstant.WEB_PATH)
@RequiredArgsConstructor
public class AccountStatementWebController {

    private final AccountService accountService;
    private final AccountStatementService accountStatementService;

    @Value("${app.aws.s3.presign-expiration-minutes:15}")
    private int linkMinutes;

    @GetMapping
    public String list(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("currentAccount", accountService.findById(accountId));
        model.addAttribute("statements", accountStatementService.findByAccountId(accountId, Math.max(page, 0)));
        model.addAttribute("linkMinutes", linkMinutes);
        return AccountStatementConstant.STATEMENTS_VIEW;
    }

    /** Used by the Copy button; session-authenticated, so the page never calls the admin-only REST API. */
    @GetMapping("/{statementId}/link")
    @ResponseBody
    public Map<String, String> link(@PathVariable UUID accountId, @PathVariable UUID statementId) {
        return accountStatementService.generatePresignedUrl(accountId, statementId);
    }

    /** Opened in a new tab by the View link: redirects to a freshly generated presigned URL. */
    @GetMapping("/{statementId}/view")
    public ResponseEntity<Void> view(@PathVariable UUID accountId, @PathVariable UUID statementId) {
        var url = accountStatementService.generatePresignedUrl(accountId, statementId)
                .get(AccountStatementConstant.PRESIGNED_URL);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
