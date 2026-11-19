package com.chitsa.accounttype.mapper;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import com.chitsa.accounttype.entity.AccountType;
import org.springframework.stereotype.Component;

/** Maps account types. Creates set the code and label on the entity before save. */
@Component
public class AccountTypeMapper {

    public AccountTypeResponseDto convertEntityToResponseDto(AccountType entity) {
        return AccountTypeResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .build();
    }
}
