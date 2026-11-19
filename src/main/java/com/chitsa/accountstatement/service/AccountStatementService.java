package com.chitsa.accountstatement.service;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

public interface AccountStatementService {

    /** Uploads the file as a media resource and links it to the account. */
    AccountStatementResponseDto create(AccountStatementRequestDto requestDto);

    /** Newest first; page size comes from {@code app.statement.page-size}. */
    Page<AccountStatementResponseDto> findByAccountId(UUID accountId, int page);

    /** Presigned link for a single statement, selected by its statement id. */
    Map<String, String> generatePresignedUrl(UUID accountId, UUID statementId);

    /** Removes the statement link, its media row, and the stored file. */
    void deleteById(UUID accountId, UUID statementId);
}
