package com.chitsa.account.mapper;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.dto.AccountResponseDto;
import com.chitsa.account.entity.Account;
import com.chitsa.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

/** The response carries only the type id here; the service fills in the type details. */
@Component
public class AccountMapper implements DtoMapper<Account, AccountRequestDto, AccountResponseDto> {

    @Override
    public Account convertDtoToEntity(AccountRequestDto dto) {
        var entity = new Account();
        entity.setUserProfileId(dto.userProfileId());
        entity.setAccountTypeId(dto.accountTypeId());
        return entity;
    }

    @Override
    public AccountResponseDto convertEntityToResponseDto(Account entity) {
        return AccountResponseDto.builder()
                .id(entity.getId())
                .userProfileId(entity.getUserProfileId())
                .createTime(entity.getCreateTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }

    /** Only the type can change, and only through the admin API. */
    @Override
    public void updateEntityFromDto(AccountRequestDto dto, Account entity) {
        entity.setAccountTypeId(dto.accountTypeId());
    }
}
