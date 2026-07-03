package com.mopl.domain.conversation.adapter.in.web.dto;

import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,
    DirectMessageDto lastMessage,
    boolean unread

) {

}
