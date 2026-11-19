package com.chitsa.account.dto;

import com.chitsa.common.validator.OnCreate;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The owner is fixed once the account exists, so userProfileId is only required on create. */
public record AccountRequestDto(
        @NotNull(groups = OnCreate.class) UUID userProfileId,
        @NotNull UUID accountTypeId) {
}
