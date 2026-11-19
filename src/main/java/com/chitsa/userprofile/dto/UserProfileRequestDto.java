package com.chitsa.userprofile.dto;

import com.chitsa.common.validator.OnCreate;
import com.chitsa.common.validator.OnUpdate;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** idpUuid and email are required on create only; update requires the names. */
public record UserProfileRequestDto(
        @Nullable @NotNull(groups = OnCreate.class) UUID idpUuid,
        @Nullable @NotBlank(groups = OnCreate.class) @Email @Size(max = 255) String email,
        @NotBlank(groups = OnUpdate.class) @Size(max = 255) String firstName,
        @NotBlank(groups = OnUpdate.class) @Size(max = 255) String lastName) {

    public static UserProfileRequestDto forUpdate(String firstName, String lastName) {
        return new UserProfileRequestDto(null, null, firstName, lastName);
    }
}
