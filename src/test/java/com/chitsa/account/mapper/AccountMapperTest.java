package com.chitsa.account.mapper;

import com.chitsa.account.dto.AccountRequestDto;
import com.chitsa.account.entity.Account;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper mapper = new AccountMapper();

    @Test
    void createCopiesTheOwnerAndType() {
        var owner = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var type = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        var entity = mapper.convertDtoToEntity(new AccountRequestDto(owner, type));

        assertThat(entity.getUserProfileId()).isEqualTo(owner);
        assertThat(entity.getAccountTypeId()).isEqualTo(type);
    }

    @Test
    void updateChangesTheTypeAndLeavesTheOwner() {
        var owner = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        var entity = new Account();
        entity.setUserProfileId(owner);
        entity.setAccountTypeId(UUID.randomUUID());

        mapper.updateEntityFromDto(new AccountRequestDto(UUID.randomUUID(), UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")), entity);

        assertThat(entity.getUserProfileId()).isEqualTo(owner);
        assertThat(entity.getAccountTypeId()).isEqualTo(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
    }

    @Test
    void responseCarriesTheOwnerAndTimes() {
        var id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        var owner = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        var created = Instant.parse("2026-01-01T00:00:00Z");
        var entity = new Account();
        entity.setId(id);
        entity.setUserProfileId(owner);
        entity.setCreateTime(created);
        entity.setUpdatedTime(created);

        var response = mapper.convertEntityToResponseDto(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.userProfileId()).isEqualTo(owner);
        assertThat(response.createTime()).isEqualTo(created);
        assertThat(response.accountType()).isNull();
    }
}
