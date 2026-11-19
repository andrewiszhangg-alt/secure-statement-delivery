package com.chitsa.accountstatement.service.impl;

import com.chitsa.account.service.AccountService;
import com.chitsa.accountstatement.config.AccountStatementProperties;
import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.entity.AccountStatement;
import com.chitsa.accountstatement.exception.AccountStatementNotFoundException;
import com.chitsa.accountstatement.mapper.AccountStatementMapper;
import com.chitsa.accountstatement.repository.AccountStatementRepository;
import com.chitsa.accountstatement.service.AccountStatementService;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.accountstatement.validator.AccountStatementValidator;
import com.chitsa.mediaresource.service.MediaResourceService;
import com.chitsa.mediaresource.util.MediaResourceUtil;
import com.chitsa.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountStatementServiceImpl implements AccountStatementService {

    private final AccountStatementRepository repository;
    private final AccountStatementMapper mapper;
    private final AccountStatementValidator validator;
    private final AccountStatementProperties properties;
    private final AccountService accountService;
    private final MediaResourceService mediaResourceService;
    private final UserProfileService userProfileService;

    @Transactional
    @Override
    public AccountStatementResponseDto create(AccountStatementRequestDto requestDto) {
        validator.validate(requestDto);

        var account = accountService.findById(requestDto.accountId());
        userProfileService.requireCompleteProfile(account.userProfileId());

        var mediaRequest = MediaResourceUtil.toCreateRequest(
                properties.getMediaContext(), generateObjectKey(requestDto), requestDto.multipartFile());
        var media = mediaResourceService.create(mediaRequest);
        var entity = mapper.convertDtoToEntity(requestDto);
        entity.setMediaResourceId(media.id());

        var saved = repository.save(entity);
        log.info(
                "Created AccountStatement id={} accountId={} mediaId={}",
                saved.getId(),
                account.id(),
                media.id());
        return mapper.convertEntityToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AccountStatementResponseDto> findByAccountId(UUID accountId, int page) {
        accountService.findById(accountId);

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(properties.getPageSize(), 1),
                Sort.by(Sort.Direction.DESC, "createTime"));
        return repository.findByAccountId(accountId, pageable).map(mapper::convertEntityToResponseDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, String> generatePresignedUrl(UUID accountId, UUID statementId) {
        var statement = findForAccount(accountId, statementId);
        var url = mediaResourceService.generatePreSignedUrlForResource(statement.getMediaResourceId());
        log.info("Generated statement presigned URL accountId={} statementId={}", accountId, statementId);
        return Map.of(AccountStatementConstant.PRESIGNED_URL, url);
    }

    /** Link row first, then the media row; the stored file goes once the transaction commits. */
    @Transactional
    @Override
    public void deleteById(UUID accountId, UUID statementId) {
        var statement = findForAccount(accountId, statementId);
        repository.delete(statement);
        repository.flush();
        mediaResourceService.deleteById(statement.getMediaResourceId());
        log.info("Deleted AccountStatement id={} accountId={}", statementId, accountId);
    }

    /** Ownership is enforced through the account lookup before the statement is loaded. */
    private AccountStatement findForAccount(UUID accountId, UUID statementId) {
        accountService.findById(accountId);
        return repository.findByIdAndAccountId(statementId, accountId)
                .orElseThrow(() -> new AccountStatementNotFoundException(
                        AccountStatementConstant.NOT_FOUND.formatted(accountId, statementId)));
    }

    /** Key is {@code <accountId>/<uuid>[.ext]}. */
    private String generateObjectKey(AccountStatementRequestDto requestDto) {
        var filename = requestDto.multipartFile().getOriginalFilename();
        var dot = filename == null ? -1 : filename.lastIndexOf('.');
        var extension = dot < 0 ? "" : filename.substring(dot);
        return requestDto.accountId() + "/" + UUID.randomUUID() + extension;
    }
}
