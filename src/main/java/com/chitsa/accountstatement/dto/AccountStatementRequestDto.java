package com.chitsa.accountstatement.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record AccountStatementRequestDto(@NotNull UUID accountId, @NotNull MultipartFile multipartFile) {
}
