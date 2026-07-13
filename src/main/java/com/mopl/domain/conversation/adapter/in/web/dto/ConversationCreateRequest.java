package com.mopl.domain.conversation.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
    @NotNull
    UUID withUserId
) {

}
