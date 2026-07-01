package com.mopl.domain.conversation.adapter.in.web.dto;

import com.mopl.global.dto.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,
    boolean hasUnread,
    Object lastMessage //TODO:OBject->DirectMessageDto구현후 교체예정

) {

}
