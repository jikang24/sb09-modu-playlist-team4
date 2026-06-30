package com.mopl.domain.notification.dto;

import com.mopl.global.response.CursorPageResponse;
import java.util.List;
import java.util.UUID;

public record CursorResponseNotificationDto(
    List<NotificationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

  public static CursorResponseNotificationDto from(CursorPageResponse<NotificationDto> response) {
    return new CursorResponseNotificationDto(
        response.data(),
        response.nextCursor(),
        response.nextIdAfter(),
        response.hasNext(),
        response.totalCount(),
        response.sortBy(),
        response.sortDirection()
    );
  }
}
