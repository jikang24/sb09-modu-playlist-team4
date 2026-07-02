package com.mopl.global.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    LocalDateTime createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content
) {

}
