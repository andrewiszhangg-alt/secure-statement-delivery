package com.chitsa.accounttype.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AccountTypeResponseDto(UUID id, String code, String label) {
}
