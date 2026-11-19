package com.chitsa.accounttype.service.impl;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.entity.AccountType;
import com.chitsa.accounttype.exception.AccountTypeNotFoundException;
import com.chitsa.accounttype.mapper.AccountTypeMapper;
import com.chitsa.accounttype.repository.AccountTypeRepository;
import com.chitsa.accounttype.service.AccountTypeService;
import com.chitsa.accounttype.util.AccountTypeConstant;
import com.chitsa.common.exception.DuplicateResourceException;
import com.chitsa.common.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AccountTypeServiceImpl implements AccountTypeService {

    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,29}");

    private final AccountTypeRepository repository;
    private final AccountTypeMapper mapper;
    private final AccountTypeCache cache;

    @Transactional(readOnly = true)
    @Override
    public List<AccountTypeResponseDto> findAll() {
        return cache.get(this::loadAll);
    }

    @Transactional(readOnly = true)
    @Override
    public AccountTypeResponseDto findById(UUID id) {
        return findAll().stream()
                .filter(type -> type.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AccountTypeNotFoundException(AccountTypeConstant.NOT_FOUND + id));
    }

    @Transactional(readOnly = true)
    @Override
    public void requireExists(UUID id) {
        findById(id);
    }

    @Transactional
    @Override
    public AccountTypeResponseDto create(String code, String label) {
        var normalized = normalizeCode(code);
        var trimmedLabel = normalizeLabel(label);
        if (repository.existsByCode(normalized)) {
            throw duplicate(normalized);
        }
        var entity = new AccountType();
        entity.setCode(normalized);
        entity.setLabel(trimmedLabel);
        try {
            var saved = repository.saveAndFlush(entity);
            dropCacheAfterCommit();
            return mapper.convertEntityToResponseDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw duplicate(normalized);
        }
    }

    private List<AccountTypeResponseDto> loadAll() {
        return repository.findAllByOrderByLabelAsc().stream()
                .map(mapper::convertEntityToResponseDto)
                .toList();
    }

    private void dropCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cache.invalidate();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.invalidate();
            }
        });
    }

    private static DuplicateResourceException duplicate(String code) {
        return new DuplicateResourceException(AccountTypeConstant.DUPLICATE + code);
    }

    private static String normalizeCode(String code) {
        ValidationUtil.requireNotBlank(code, AccountTypeConstant.CODE_REQUIRED);
        var normalized = code.trim().toUpperCase(Locale.ROOT);
        ValidationUtil.require(CODE.matcher(normalized).matches(), AccountTypeConstant.CODE_INVALID);
        return normalized;
    }

    private static String normalizeLabel(String label) {
        ValidationUtil.requireNotBlank(label, AccountTypeConstant.LABEL_REQUIRED);
        var trimmed = label.trim();
        ValidationUtil.require(trimmed.length() <= 100, AccountTypeConstant.LABEL_INVALID);
        return trimmed;
    }
}
