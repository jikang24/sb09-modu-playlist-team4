package com.mopl.domain.conversation.adapter.in.web.dto;

import java.util.UUID;

public record ConversationSearchRequest(
    String keywordLike,
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    String sortDirection
) {

}
