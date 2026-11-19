package com.chitsa.account.service.impl;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.entity.Account;
import com.chitsa.account.exception.AccountNotFoundException;
import com.chitsa.account.mapper.AccountMapper;
import com.chitsa.account.repository.AccountRepository;
import com.chitsa.account.service.AccountAccessGuard;
import com.chitsa.account.service.AccountService;
import com.chitsa.account.util.AccountConstant;
import com.chitsa.account.validator.AccountValidator;
import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.userprofile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final AccountMapper mapper;
    private final AccountValidator validator;
    private final AccountAccessGuard accessGuard;
    private final UserProfileService userProfileService;
    private final AccountTypeService accountTypeService;

    @Transactional
    @Override
    public AccountResponseDto create(AccountRequestDto requestDto) {
        validator.validateForCreate(requestDto);
        accessGuard.assertCanAccessProfile(requestDto.userProfileId());
        userProfileService.requireCompleteProfile(requestDto.userProfileId());
        accountTypeService.requireExists(requestDto.accountTypeId());

        var saved = repository.save(mapper.convertDtoToEntity(requestDto));
        log.info("Created Account id={} typeId={}", saved.getId(), saved.getAccountTypeId());
        return withType(saved);
    }

    /** Admin type change; the owner never changes. */
    @Transactional
    @Override
    public AccountResponseDto update(UUID id, AccountRequestDto requestDto) {
        validator.validate(requestDto);
        var entity = findEntityById(id);
        accessGuard.assertCanAccess(entity);
        accountTypeService.requireExists(requestDto.accountTypeId());

        mapper.updateEntityFromDto(requestDto, entity);
        log.info("Updated Account id={} typeId={}", id, entity.getAccountTypeId());
        return withType(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public AccountResponseDto findById(UUID id) {
        var entity = findEntityById(id);
        accessGuard.assertCanAccess(entity);
        return withType(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AccountResponseDto> findByUserProfileId(UUID userProfileId) {
        accessGuard.assertCanAccessProfile(userProfileId);
        return withTypes(repository.findByUserProfileId(userProfileId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<AccountResponseDto> findAll() {
        accessGuard.assertAdmin();
        return withTypes(repository.findAll());
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        var entity = findEntityById(id);
        accessGuard.assertCanAccess(entity);
        repository.delete(entity);
        repository.flush();
        log.info("Deleted Account id={}", id);
    }

    private List<AccountResponseDto> withTypes(List<Account> accounts) {
        var types = accountTypeService.findAll().stream()
                .collect(Collectors.toMap(AccountTypeResponseDto::id, Function.identity()));
        return accounts.stream()
                .map(account -> withType(account, types.get(account.getAccountTypeId())))
                .toList();
    }

    private AccountResponseDto withType(Account account) {
        return withType(account, accountTypeService.findById(account.getAccountTypeId()));
    }

    private AccountResponseDto withType(Account account, AccountTypeResponseDto type) {
        return mapper.convertEntityToResponseDto(account).toBuilder().accountType(type).build();
    }

    private Account findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstant.NOT_FOUND + id));
    }
}
