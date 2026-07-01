package com.mopl.domain.conversation.application.port.in;

import com.mopl.domain.conversation.domain.Conversation;
import java.util.UUID;

public interface GetConversationUseCase {
  Conversation getById(UUID conversationId,UUID myId);
  Conversation getByParticipant(UUID myId, UUID withUserId);

}
