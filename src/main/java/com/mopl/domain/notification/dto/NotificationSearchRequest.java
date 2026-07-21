package com.mopl.domain.notification.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record NotificationSearchRequest(
    String cursor,
    UUID idAfter,
    int limit,
    SortDirection sortDirection,
    NotificationSortBy sortBy
) {
}
