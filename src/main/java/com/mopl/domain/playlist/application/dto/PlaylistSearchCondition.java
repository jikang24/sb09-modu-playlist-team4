package com.mopl.domain.playlist.application.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record PlaylistSearchCondition(
    String keywordLike,
    UUID ownerIdEqual,
    UUID subscriberIdEqual,
    String cursor,
    UUID idAfter,
    int limit,
    SortBy sortBy,
    SortDirection sortDirection
) {
}