package com.chitsa.accountstatement.mapper;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.dto.AccountStatementResponseDto;
import com.chitsa.accountstatement.entity.AccountStatement;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import com.chitsa.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountStatementMapper
        implements DtoMapper<AccountStatement, AccountStatementRequestDto, AccountStatementResponseDto> {

    /** The media link is attached by the service once the file is stored. */
    @Override
    public AccountStatement convertDtoToEntity(AccountStatementRequestDto dto) {
        var entity = new AccountStatement();
        entity.setAccountId(dto.accountId());
        return entity;
    }

    @Override
    public AccountStatementResponseDto convertEntityToResponseDto(AccountStatement entity) {
        return AccountStatementResponseDto.builder()
                .id(entity.getId())
                .createTime(entity.getCreateTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }

    /** Statements are immutable after create. */
    @Override
    public void updateEntityFromDto(AccountStatementRequestDto dto, AccountStatement entity) {
        throw new UnsupportedOperationException(AccountStatementConstant.UPDATE_NOT_SUPPORTED);
    }
}
