package com.chitsa.accountstatement.mapper;

import com.chitsa.accountstatement.dto.AccountStatementRequestDto;
import com.chitsa.accountstatement.entity.AccountStatement;
import com.chitsa.accountstatement.util.AccountStatementConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountStatementMapperTest {

    private final AccountStatementMapper mapper = new AccountStatementMapper();

    @Test
    void createCopiesTheAccountAndLeavesTheMediaLinkEmpty() {
        var accountId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "statement".getBytes());

        var entity = mapper.convertDtoToEntity(new AccountStatementRequestDto(accountId, file));

        assertThat(entity.getAccountId()).isEqualTo(accountId);
        assertThat(entity.getMediaResourceId()).isNull();
    }

    @Test
    void responseCarriesTheStatementIdAndTimes() {
        var id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        var created = Instant.parse("2026-01-01T00:00:00Z");
        var entity = new AccountStatement();
        entity.setId(id);
        entity.setCreateTime(created);
        entity.setUpdatedTime(created);

        var response = mapper.convertEntityToResponseDto(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.createTime()).isEqualTo(created);
        assertThat(response.updatedTime()).isEqualTo(created);
    }

    @Test
    void updateIsRejected() {
        assertThatThrownBy(() -> mapper.updateEntityFromDto(null, new AccountStatement()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(AccountStatementConstant.UPDATE_NOT_SUPPORTED);
    }
}
