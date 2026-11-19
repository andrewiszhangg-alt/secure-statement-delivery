package com.chitsa.accounttype.mapper;

import com.chitsa.accounttype.dto.AccountTypeRequestDto;
import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.entity.AccountType;
import com.chitsa.accounttype.util.AccountTypeConstant;
import com.chitsa.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountTypeMapper implements DtoMapper<AccountType, AccountTypeRequestDto, AccountTypeResponseDto> {

    @Override
    public AccountType convertDtoToEntity(AccountTypeRequestDto dto) {
        var entity = new AccountType();
        entity.setCode(dto.code());
        entity.setLabel(dto.label());
        return entity;
    }

    @Override
    public AccountTypeResponseDto convertEntityToResponseDto(AccountType entity) {
        return AccountTypeResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .build();
    }

    /** Account types are not edited after they are saved. */
    @Override
    public void updateEntityFromDto(AccountTypeRequestDto dto, AccountType entity) {
        throw new UnsupportedOperationException(AccountTypeConstant.UPDATE_NOT_SUPPORTED);
    }
}
