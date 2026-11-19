package com.chitsa.account.dto;

import com.chitsa.accounttype.dto.AccountTypeResponseDto;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record AccountResponseDto(
        UUID id,
        UUID userProfileId,
        AccountTypeResponseDto accountType,
        Instant createTime,
        Instant updatedTime) {
}
