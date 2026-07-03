package com.mopl.domain.watchingsession.dto;

import com.mopl.global.dto.ContentSummary;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto(
    UUID id,
    Instant createdAt,
    UserSummary watcher,
    ContentSummary content
) {}