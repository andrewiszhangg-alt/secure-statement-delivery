package com.chitsa.account.service;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.dto.AccountResponseDto;
import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto create(AccountRequestDto requestDto);

    AccountResponseDto update(UUID id, AccountRequestDto requestDto);

    AccountResponseDto findById(UUID id);

    List<AccountResponseDto> findByUserProfileId(UUID userProfileId);

    /** Every account. Admins only. */
    List<AccountResponseDto> findAll();

    void deleteById(UUID id);
}
