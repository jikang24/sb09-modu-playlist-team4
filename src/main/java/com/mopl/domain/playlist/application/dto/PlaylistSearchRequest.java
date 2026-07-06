package com.mopl.domain.playlist.application.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record PlaylistSearchRequest(
    String keywordLike,
    UUID ownerIdEqual,
    UUID subscriberIdEqual,
    String cursor,
    UUID idAfter,
    int limit,
    SortBy sortBy,
    SortDirection sortDirection
) {

  public PlaylistSearchCondition toCondition() {
    return new PlaylistSearchCondition(
        keywordLike,
        ownerIdEqual,
        subscriberIdEqual,
        cursor,
        idAfter,
        limit,
        sortBy != null ? sortBy : SortBy.updatedAt,
        sortDirection != null ? sortDirection : SortDirection.DESCENDING
    );
  }
}