package com.chitsa.mediaresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record MediaResourceRequestDto(
        @NotBlank String context,
        @NotBlank String objectKey,
        @NotNull MultipartFile multipartFile) {
}
