package com.mopl.domain.conversation.adapter.in.web.dto;

import java.util.UUID;

public record ConversationCreateRequest(
    UUID withUserId
) {

}
