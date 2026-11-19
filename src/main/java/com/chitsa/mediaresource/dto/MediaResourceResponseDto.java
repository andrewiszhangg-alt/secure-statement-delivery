package com.chitsa.mediaresource.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MediaResourceResponseDto(
        UUID id,
        String context,
        String objectKey,
        String url,
        String contentType,
        String originalFilename,
        Instant createTime,
        Instant updatedTime) {
}
