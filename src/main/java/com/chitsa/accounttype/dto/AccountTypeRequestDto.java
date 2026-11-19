package com.chitsa.accounttype.dto;

/** Code and label are already normalized by the service. */
public record AccountTypeRequestDto(String code, String label) {
}
