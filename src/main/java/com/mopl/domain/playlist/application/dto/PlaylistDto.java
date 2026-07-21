package com.mopl.domain.playlist.application.dto;

import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
    UUID id,
    UserSummary owner,
    String title,
    String description,
    Instant updatedAt,
    long subscriberCount,
    boolean subscribedByMe,
    List<ContentSummary> contents
) {
}
