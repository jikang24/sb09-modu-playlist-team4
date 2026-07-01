package com.mopl.domain.dm.application.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record DirectMessageSearchCondition(
    String cursor,
    UUID idAfter,
    int limit,
    SortDirection sortDirection,
    String sortBy
) {
  public boolean isFirstPage() {
    return cursor == null && idAfter==null;
  }

}
