package com.chitsa.userprofile.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserProfileResponseDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Instant createTime,
        Instant updatedTime,
        boolean profileComplete) {
}
