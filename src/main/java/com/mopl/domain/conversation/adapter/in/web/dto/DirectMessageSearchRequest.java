package com.mopl.domain.conversation.adapter.in.web.dto;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;

public record DirectMessageSearchRequest(
    String cursor,
    UUID idAfter,
    int limit,
    String sortBy,
    SortDirection sortDirection
) {

}
