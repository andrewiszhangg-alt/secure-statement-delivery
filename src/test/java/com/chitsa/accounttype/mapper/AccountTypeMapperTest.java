package com.chitsa.accounttype.mapper;

import com.chitsa.accounttype.dto.AccountTypeRequestDto;
import com.chitsa.accounttype.entity.AccountType;
import com.chitsa.accounttype.util.AccountTypeConstant;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTypeMapperTest {

    private final AccountTypeMapper mapper = new AccountTypeMapper();

    @Test
    void createCopiesTheCodeAndLabel() {
        var entity = mapper.convertDtoToEntity(new AccountTypeRequestDto("SAVINGS", "Savings"));

        assertThat(entity.getCode()).isEqualTo("SAVINGS");
        assertThat(entity.getLabel()).isEqualTo("Savings");
    }

    @Test
    void mapsTheEntityToTheResponse() {
        var id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var entity = new AccountType();
        entity.setId(id);
        entity.setCode("SAVINGS");
        entity.setLabel("Savings");

        var response = mapper.convertEntityToResponseDto(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.code()).isEqualTo("SAVINGS");
        assertThat(response.label()).isEqualTo("Savings");
    }

    @Test
    void updateIsRejected() {
        assertThatThrownBy(() -> mapper.updateEntityFromDto(
                new AccountTypeRequestDto("SAVINGS", "Savings"), new AccountType()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(AccountTypeConstant.UPDATE_NOT_SUPPORTED);
    }
}
