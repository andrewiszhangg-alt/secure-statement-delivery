package com.chitsa.accounttype.service;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;

import java.util.List;
import java.util.UUID;

public interface AccountTypeService {

    /** All account types, sorted by label. */
    List<AccountTypeResponseDto> findAll();

    AccountTypeResponseDto findById(UUID id);

    /** Throws AccountTypeNotFoundException when the type does not exist. */
    void requireExists(UUID id);

    /** Saves a type and drops the catalog cache on this instance. */
    AccountTypeResponseDto create(String code, String label);
}
