package com.chitsa.accountstatement.controller;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(AccountStatementConstant.API_PATH)
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService accountStatementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountStatementResponseDto> create(
            @PathVariable UUID accountId,
            @RequestPart("file") MultipartFile file) {
        var request = new AccountStatementRequestDto(accountId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountStatementService.create(request));
    }

    @GetMapping
    public Page<AccountStatementResponseDto> findByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page) {
        return accountStatementService.findByAccountId(accountId, page);
    }

    @GetMapping("/{statementId}/presigned-url")
    public Map<String, String> generatePresignedUrl(
            @PathVariable UUID accountId,
            @PathVariable UUID statementId) {
        return accountStatementService.generatePresignedUrl(accountId, statementId);
    }

    @DeleteMapping("/{statementId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable UUID accountId,
            @PathVariable UUID statementId) {
        accountStatementService.deleteById(accountId, statementId);
        return ResponseEntity.noContent().build();
    }
}
