package com.chitsa.accountstatement.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/** Only what the client needs: the statement id (used to request a link) and timestamps. */
@Builder
public record AccountStatementResponseDto(UUID id, Instant createTime, Instant updatedTime) {
}
