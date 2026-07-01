package com.mopl.domain.conversation.application.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record ConversationSearchCondition(
    String keywordLike,
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    SortDirection sortDirection
) {

  public boolean hasKeyword() {
    return keywordLike != null && !keywordLike.isBlank();
  }
  public boolean hasFirstPage() {
    return cursor == null && idAfter==null;
  }

}
