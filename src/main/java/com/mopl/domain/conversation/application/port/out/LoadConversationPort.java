package com.mopl.domain.conversation.application.port.out;

import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.global.response.CursorPageResponse;
import java.util.Optional;
import java.util.UUID;

public interface LoadConversationPort {
  Optional<Conversation> findById(UUID id);
  Optional<Conversation> findByParticipants(UUID myId, UUID withUserId);
  CursorPageResponse<Conversation> findList(UUID myId, ConversationSearchCondition condition);
}
